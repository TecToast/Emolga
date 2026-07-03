package de.tectoast.emolga.domain.league.doc.service

import de.tectoast.emolga.domain.game.model.FullInputGame
import de.tectoast.emolga.domain.game.model.GameSource
import de.tectoast.emolga.domain.game.model.SingleGame
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.HideGamesConfig
import de.tectoast.emolga.domain.league.doc.model.HideGamesInsertData
import de.tectoast.emolga.domain.league.doc.service.provider.event.AnalysisEventProvider
import de.tectoast.emolga.domain.league.doc.service.provider.monname.MonNameProviderFactory
import de.tectoast.emolga.domain.league.doc.service.provider.order.MonsDocOrderDispatcher
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import de.tectoast.emolga.domain.league.gamedata.repository.LeagueEventRepository
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameService
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.transaction.service.TransactionExecutionService
import de.tectoast.emolga.domain.league.util.model.LeagueResult
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.sheetupdate.SpreadsheetService
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class DocEntryService(
    private val predictionGameService: PredictionGameService,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val replayDataRepo: GameDataRepository,
    private val statProcessorService: StatProcessorService,
    private val monNameProviderFactory: MonNameProviderFactory,
    private val tierlistRepo: TierlistRepository,
    private val analysisEventProvider: AnalysisEventProvider,
    private val spreadsheetService: SpreadsheetService,
    private val leaguePickRepo: LeaguePickRepository,
    private val monsOrderDispatcher: MonsDocOrderDispatcher,
    private val leagueEventRepo: LeagueEventRepository,
    private val repeatTaskScheduler: RepeatTaskScheduler,
    private val transactionExecutionService: TransactionExecutionService,
    private val hideGamesInsertFlow: HideGamesInsertFlow,
) {
    private val logger = KotlinLogging.logger {}
    suspend fun checkAndProcess(
        leagueName: String, data: FullGameData, ignoreHideGames: Boolean = false, withSort: Boolean = true
    ) {
        val config = leagueConfigRepo.getConfig(leagueName)
        val store = config.gameDataStore
        val week = data.week
        config.predictionGame?.let { _ ->
            predictionGameService.lockButtonsIndividual(leagueName, week, data.battleIndex)
            predictionGameService.updateCorrectBattles(leagueName, week, data.battleIndex, data.winnerIdx)
        }
        val hideGamesConfig = config.hideGames?.takeUnless { ignoreHideGames }
        replayDataRepo.storeFullGameData(leagueName, data)
        if (store != null) {
            val currentWeek =
                repeatTaskScheduler.getUpcomingNumber(RepeatTaskType.RegisterInDoc(leagueName, data.battleIndex))
                    ?: Int.MAX_VALUE
            if (currentWeek <= week) return
        } else if (hideGamesConfig != null) {
            if (week in hideGamesConfig.weeks) {
                replayDataRepo.getFullGameDataForWeekIfAllPresent(leagueName, week)?.let { allData ->
                    val guild = leagueCoreRepo.getScalarLeagueData(leagueName).guild
                    hideGamesInsertFlow.tryEmit(
                        allData.toHideGamesInsertData(leagueName, hideGamesConfig, guild)
                    )
                }
                return
            }
        }
        process(leagueName, data, withSort)
    }

    private fun List<FullGameData>.toHideGamesInsertData(
        leagueName: String,
        hideGames: HideGamesConfig,
        guild: Long
    ): HideGamesInsertData = HideGamesInsertData(
        this.map {
            FullInputGame(it.games.map { gd ->
                SingleGame(
                    GameSource.Direct(
                        LeagueResult(
                            leagueName, it.uindices
                        )
                    ), is4v4 = false, winnerIndex = gd.winnerIndex, kd = gd.kd
                )
            }, false)
        }, hideGames, guild
    )

    suspend fun process(leagueName: String, fullGameData: FullGameData, withSort: Boolean = true) {
        if (fullGameData.games.isEmpty()) return
        if (fullGameData.games.any { game -> game.kd.size != 2 }) {
            logger.warn("Skipping analysis for league $leagueName week ${fullGameData.week} battle ${fullGameData.battleIndex} due to invalid game data. (Not 1v1 games)")
            return
        }
        val leagueData = leagueCoreRepo.getScalarLeagueData(leagueName)
        val config = leagueConfigRepo.getConfig(leagueName)
        val guild = leagueData.guild
        val lang = tierlistRepo.getAllMetasForGuild(guild).firstOrNull()?.language ?: Language.ENGLISH
        val picks = leaguePickRepo.getAllPicks(leagueName)
        spreadsheetService.updateSheet(leagueData.sheetId, wait = false) {
            val events = statProcessorService.execute(
                AdditionalDataProvider(
                    monNameProviderFactory.getMonNameProvider(guild, lang), analysisEventProvider
                ),
                sheet = this,
                fullGameData,
                leagueName,
                picks,
                { pickList -> monsOrderDispatcher.getDocSortedMons(config.monsDocOrder, guild, config, pickList) },
                config.statProcessors
            )
            leagueEventRepo.addEvents(events)
            transactionExecutionService.registerTransactions(leagueName, fullGameData.week, fullGameData.uindices)
            if (withSort) {
                withRunnable(3.seconds) {
                    sort()
                }
            }
        }
    }

    private fun sort() {
        // later
    }
}