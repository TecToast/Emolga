package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.TierlistMeta
import de.tectoast.emolga.database.exposed.TierlistRepository
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.error
import de.tectoast.emolga.utils.json.getOrReturn
import de.tectoast.emolga.utils.json.isError
import de.tectoast.emolga.utils.json.success
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable
sealed interface DraftInput {
    val pokemon: DraftName
    val freesPokemon: String?
}

@Serializable
data class PickInput(
    override val pokemon: DraftName,
    val tier: String?,
    val free: Boolean,
    val tera: Boolean = false,
    val noCost: Boolean = false
) : DraftInput {
    override val freesPokemon = null
}

@Serializable
data class SwitchInput(val oldmon: String, override val pokemon: DraftName) : DraftInput {
    override val freesPokemon = oldmon

}

@Serializable
data class BanInput(override val pokemon: DraftName) : DraftInput {
    override val freesPokemon = null

}

enum class DraftMessageType {
    REGULAR, QUEUE, RANDOM, ACCEPT, REROLL
}


data class DraftRunContext(
    val league: DraftRelevantLeagueData, val config: LeagueConfig, val tierlistMeta: TierlistMeta, var activeIdx: Int
)

@Single
class DraftService(
    val tx: TransactionRunner,
    val leagueCoreRepo: LeagueCoreRepository,
    val leagueConfigRepo: LeagueConfigRepository,
    val tierlistRepository: TierlistRepository,
    val draftPermissionService: DraftPermissionService,
    val draftExecutionService: DraftExecutionService,
    val draftExecutionHandler: DraftExecutionHandler,
    val draftTimerService: DraftTimerService,
    dispatcher: CoroutineDispatcher
) : StartupTask {
    private val draftTimerScope = createCoroutineScope("DraftServiceTimerWorker", dispatcher)
    private val logger = KotlinLogging.logger {}
    override suspend fun onStartup() {
        launchCollectTask(draftTimerService.expiredTimerEvents, ::collectExpiredTimerEvent)
    }

    private fun launchCollectTask(events: Flow<String>, handler: suspend (String) -> Unit) {
        draftTimerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            events.collect(handler)
        }
    }

    private suspend fun collectExpiredTimerEvent(leagueName: String) {
        val result = executeSkip(leagueName, SkipReason.REALTIMER)
        if (result.isError()) {
            logger.error {
                "Failed to execute timer skip for league $leagueName: ${result.message.translateTo(K18N_DEFAULT_LANGUAGE)}"
            }
        }
    }

    suspend fun startDraft(tc: Long) = tx {
        // TODO: Set sessionId to max of current sessionIds + 1
    }

    suspend fun executeNormal(
        input: DraftInput,
        type: DraftMessageType,
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>,
        validationCompleteCallback: suspend () -> Unit = { }
    ) = tx {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(tcId) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        val pickContext = draftPermissionService.checkPickPermission(leagueData, userId, roleIds)
            .getOrReturn<PickContext, Unit> { return@tx it }
        val config = leagueConfigRepo.getConfig(leagueData.leagueName)
        val tierlistMeta =
            tierlistRepository.getMeta(leagueData.guild, config.tlIdentifier) ?: return@tx K18n_NoTierlist.error<Unit>()
        val ctx = DraftRunContext(leagueData, config, tierlistMeta, pickContext.currentIdx(leagueData))
        val result = draftExecutionService.processDraftInput(
            ctx, input, type, pickContext
        ).getOrReturn<DraftExecution, Unit> { return@tx it }
        validationCompleteCallback()
        handleDraftExecutionAndSave(result, ctx)
        Unit.success()
    }

    suspend fun executeSkip(leagueName: String, reason: SkipReason, skippedBy: Long? = null) = tx {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(leagueName) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        if (reason == SkipReason.REALTIMER && !draftTimerService.isValidTimerExecution(leagueData.draftData.timer.cooldown)) {
            return@tx Unit.success()
        }
        val config = leagueConfigRepo.getConfig(leagueData.leagueName)
        val tierlistMeta =
            tierlistRepository.getMeta(leagueData.guild, config.tlIdentifier) ?: return@tx K18n_NoTierlist.error<Unit>()
        val ctx = DraftRunContext(leagueData, config, tierlistMeta, leagueData.currentIdx)
        val result = draftExecutionService.processSkip(ctx, reason, skippedBy)
        handleDraftExecutionAndSave(result, ctx)
        Unit.success()
    }

    private suspend fun handleDraftExecutionAndSave(result: DraftExecution, ctx: DraftRunContext) {
        when(result.timerOption) {
            TimerOption.RESTART -> draftTimerService.restartTimer(ctx, delayData = null)
            TimerOption.CANCEL -> draftTimerService.cancelTimer(ctx.league.leagueName)
            TimerOption.KEEP -> {}
        }
        draftExecutionHandler.handleDraftExecution(result, ctx)
        leagueCoreRepo.updateDraftData(ctx.league.leagueName, ctx.league.draftData)
    }

}

sealed interface PickContext {

    fun currentIdx(data: DraftRelevantLeagueData): Int

    data object RegularTurn : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = data.currentIdx
    }

    data class AfterDraftUnordered(val idx: Int) : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = idx
    }

    data class InBetweenPick(val idx: Int, val isActualCurrent: Boolean) : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = idx
    }

}


sealed interface NextPlayerData {
    data object Normal : NextPlayerData
    data object InBetween : NextPlayerData
    data class Moved(val reason: SkipReason, val skippedUser: Int, val skippedBy: Long? = null) : NextPlayerData

}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isNormalPick(): Boolean {
    contract {
        returns(true) implies (this@isNormalPick is NextPlayerData.Normal)
    }
    return this is NextPlayerData.Normal
}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isMoved(): Boolean {
    contract {
        returns(true) implies (this@isMoved is NextPlayerData.Moved)
    }
    return this is NextPlayerData.Moved
}

enum class SkipReason {
    REALTIMER, SKIP
}

interface DisplayHelper {
    suspend fun buildAnnounceData(idx: Int, withTimerAnnounce: Boolean): K18nMessage
    suspend fun getPingForUser(idx: Int): MessageMentionData
}

data class TimerSkipData(
    val result: TimerSkipResult,
    val message: (suspend (DisplayHelper) -> K18nMessage)? = null,
    val cancelTimer: Boolean = false
)

enum class TimerSkipResult {
    NEXT, SAME, NOCONCRETE
}

fun TimerSkipResult.defaultData() = TimerSkipData(this)

