package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
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


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}