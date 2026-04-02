package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.PredictionGameAnalyseService
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.json.msg
import org.koin.core.annotation.Single

@Single
class PredictionGameCommand(
    top10Command: Top10Command,
    selfCommand: Self,
    checkMissingCommand: CheckMissing,
    ownVotesCommand: OwnVotes
) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("predictiongame", K18n_PredictionGameCommand.Help)) {

    override val children = listOf(top10Command, selfCommand, checkMissingCommand, ownVotesCommand)

    @Single
    class Top10Command(val analysisService: PredictionGameAnalyseService) :
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
    class Self(val analysisService: PredictionGameAnalyseService) :
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
    class CheckMissing(val analysisService: PredictionGameAnalyseService) : CommandFeature<CheckMissing.Args>(
        ::Args,
        CommandSpec("checkmissing", K18n_PredictionGameCommand.CheckMissingHelp)
    ) {
        class Args : Arguments() {
            val gameday by int("Week", K18n_PredictionGameCommand.CheckMissingArgGameday)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            iData.reply(
                analysisService.getMissingVotesForGameday(iData.gid, e.gameday, iData.user).msg(),
                ephemeral = true
            )
        }
    }

    @Single
    class OwnVotes(val analysisService: PredictionGameAnalyseService) :
        CommandFeature<OwnVotes.Args>(::Args, CommandSpec("ownvotes", K18n_PredictionGameCommand.OwnVotesHelp)) {

        class Args : Arguments() {
            val leaguename by string("Liga", K18n_PredictionGameCommand.OwnVotesArgLeague) {
                slashCommand { string, event ->
                    val gid = event.guild!!.idLong
                    val names = leagueNameCache.getOrPut(gid) {
                        mdb.leaguesByGuild(event.guild!!.idLong).map { it.displayName }
                    }
                    names.filter { it.contains(string, true) }
                }
            }
            val gameday by int("Week", K18n_PredictionGameCommand.OwnVotesArgGameday)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            iData.reply(
                analysisService.getOwnVotesForLeagueAndGameday(
                    iData.gid,
                    e.leaguename,
                    e.gameday,
                    iData.user,
                ).msg(),
                ephemeral = true
            )
        }

        companion object {
            val leagueNameCache = SizeLimitedMap<Long, List<String>>(100)
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}
