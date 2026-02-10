package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.TipGameAnalyseService

object TipGameCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("tipgame", "Siehe die Tippspiel-Ergebnisse ein")) {

    object Top10Command : CommandFeature<NoArgs>(NoArgs(), CommandSpec("top10", "Zeige die Top 10 an")) {


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            iData.reply("# Top 10 des Servers\n" + TipGameAnalyseService.getTop10OfGuild(iData.gid))
        }
    }

    object Self :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("self", "Schaue deine eigenen Tippspiel-Ergebnisse an")) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            val aboveAndBelow = TipGameAnalyseService.getTipGameStatsWithAboveAndBelow(gid, uid)
                ?: return iData.reply("Du hast noch keine Tipps abgegeben.", ephemeral = true)
            val resultsPerLeague = TipGameAnalyseService.getUserTipGameStatsPerLeague(
                gid, uid
            )
            iData.reply(
                "# Du im Vergleich\n$aboveAndBelow\n# Deine Statistik pro Liga\n$resultsPerLeague", ephemeral = true
            )
        }
    }

    object CheckMissing : CommandFeature<CheckMissing.Args>(
        ::Args,
        CommandSpec("checkmissing", "Prüfe auf fehlende Tipps an einem Spieltag")
    ) {
        class Args : Arguments() {
            val gameday by int("Spieltag", "Der Spieltag, der geprüft werden soll")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            val gid = iData.gid
            val uid = iData.user
            iData.reply(
                TipGameAnalyseService.getMissingVotesForGameday(gid, e.gameday, uid), ephemeral = true
            )
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}