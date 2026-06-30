package de.tectoast.emolga.features.league.prediction

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameAnalyseTextService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import de.tectoast.emolga.utils.newThreadSafeCache
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class PredictionGameCommand(
    top10Command: Top10Command,
    selfCommand: Self,
    checkMissingCommand: CheckMissing,
    ownVotesCommand: OwnVotes
) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("predictiongame", K18n_PredictionGameCommand.Help)) {

    override val children = listOf(top10Command, selfCommand, checkMissingCommand, ownVotesCommand)

    @Single
    class Top10Command(private val analysisService: PredictionGameAnalyseTextService) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("top10", K18n_PredictionGameCommand.Top10Help)) {


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            iData.reply(
                analysisService.getTopNOfGuild(iData.gid, 10).msg()
            )
        }
    }

    @Single
    class Self(private val analysisService: PredictionGameAnalyseTextService) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("self", K18n_PredictionGameCommand.SelfHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            iData.reply(
                analysisService.selfData(iData.gid, iData.user).msg(), ephemeral = true
            )
        }
    }

    @Single
    class CheckMissing(private val analysisService: PredictionGameAnalyseTextService) :
        CommandFeature<CheckMissing.Args>(
            ::Args,
            CommandSpec("checkmissing", K18n_PredictionGameCommand.CheckMissingHelp)
        ) {
        class Args : Arguments() {
            var week by int("Week", K18n_PredictionGameCommand.CheckMissingArgWeek)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            iData.reply(
                analysisService.getMissingVotesForWeek(iData.gid, e.week, iData.user).msg(),
                ephemeral = true
            )
        }
    }

    @Single
    class OwnVotes(private val analysisService: PredictionGameAnalyseTextService) :
        CommandFeature<OwnVotes.Args>(::Args, CommandSpec("ownvotes", K18n_PredictionGameCommand.OwnVotesHelp)) {

        class Args : Arguments() {

            val leagueCoreRepo: LeagueCoreRepository by inject()

            val leaguename by string("Liga", K18n_PredictionGameCommand.OwnVotesArgLeague) {
                slashCommand { string, event ->
                    val gid = event.guild!!.idLong
                    val names = leagueNameCache.getOrPut(gid) {
                        leagueCoreRepo.getLeagueDisplayNames(gid)
                    }
                    names.filter { it.contains(string, true) }
                }
            }
            val week by int("Week", K18n_PredictionGameCommand.OwnVotesArgWeek)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            iData.reply(
                analysisService.getOwnVotesForLeagueAndWeek(
                    iData.gid,
                    e.leaguename,
                    e.week,
                    iData.user,
                ).msg(),
                ephemeral = true
            )
        }

        companion object {
            val leagueNameCache = newThreadSafeCache<Long, List<String>>(100)
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}