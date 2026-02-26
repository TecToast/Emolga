package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.PredictionGameAnalyseService
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.mdb

object PredictionGameCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("predictiongame", K18n_PredictionGameCommand.Help)) {

    object Top10Command : CommandFeature<NoArgs>(NoArgs(), CommandSpec("top10", K18n_PredictionGameCommand.Top10Help)) {


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            iData.reply(
                PredictionGameAnalyseService.getTopNOfGuild(iData.gid, 10)
            )
        }
    }

    object Self :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("self", K18n_PredictionGameCommand.SelfHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            val aboveAndBelow = PredictionGameAnalyseService.getStatsWithAboveAndBelow(gid, uid)
                ?: return iData.reply(K18n_PredictionGameCommand.SelfNoPredictions, ephemeral = true)
            val resultsPerLeague = PredictionGameAnalyseService.getUserStatsPerLeague(
                gid, uid
            )

            iData.reply(
                K18n_PredictionGameCommand.SelfSuccess(aboveAndBelow, resultsPerLeague), ephemeral = true
            )
        }
    }

    object CheckMissing : CommandFeature<CheckMissing.Args>(
        ::Args,
        CommandSpec("checkmissing", K18n_PredictionGameCommand.CheckMissingHelp)
    ) {
        class Args : Arguments() {
            val gameday by int("Week", K18n_PredictionGameCommand.CheckMissingArgGameday)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            iData.reply(
                PredictionGameAnalyseService.getMissingVotesForGameday(gid, e.gameday, uid, iData.language),
                ephemeral = true
            )
        }
    }

    object OwnVotes :
        CommandFeature<OwnVotes.Args>(::Args, CommandSpec("ownvotes", K18n_PredictionGameCommand.OwnVotesHelp)) {
        val leagueNameCache = SizeLimitedMap<Long, List<String>>(100)

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
            val gid = iData.gid
            val uid = iData.user
            iData.reply(
                PredictionGameAnalyseService.getOwnVotesForLeagueAndGameday(
                    gid,
                    e.leaguename,
                    e.gameday,
                    uid,
                    iData.language
                ),
                ephemeral = true
            )
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}
