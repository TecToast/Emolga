package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.TipGameAnalyseService

object TipGameCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("tipgame", K18n_TipGameCommand.Help)) {

    object Top10Command : CommandFeature<NoArgs>(NoArgs(), CommandSpec("top10", K18n_TipGameCommand.Top10Help)) {


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            iData.reply(
                K18n_TipGameCommand.Top10Success(
                    TipGameAnalyseService.getTop10OfGuild(
                        iData.gid,
                        iData.language
                    )
                )
            )
        }
    }

    object Self :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("self", K18n_TipGameCommand.SelfHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            val aboveAndBelow = TipGameAnalyseService.getTipGameStatsWithAboveAndBelow(gid, uid)
                ?: return iData.reply(K18n_TipGameCommand.SelfNoTips, ephemeral = true)
            val resultsPerLeague = TipGameAnalyseService.getUserTipGameStatsPerLeague(
                gid, uid
            )

            iData.reply(
                K18n_TipGameCommand.SelfSuccess(aboveAndBelow, resultsPerLeague), ephemeral = true
            )
        }
    }

    object CheckMissing : CommandFeature<CheckMissing.Args>(
        ::Args,
        CommandSpec("checkmissing", K18n_TipGameCommand.CheckMissingHelp)
    ) {
        class Args : Arguments() {
            val gameday by int("Spieltag", K18n_TipGameCommand.CheckMissingArgGameday)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            iData.reply(
                TipGameAnalyseService.getMissingVotesForGameday(gid, e.gameday, uid, iData.language), ephemeral = true
            )
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}