package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.germanTypeList
import de.tectoast.emolga.commands.invoke
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL

object TeraMDLCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("teramdl", "Randomized den Tera-Typen", Constants.G.VIP)) {

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        if (d !is MDL) {
            reply("Dieser Befehl funktioniert nur im MDL Draft!")
            return
        }
        if (!d.isLastRound) {
            reply("Dieser Befehl kann nur in der letzten Runde verwendet werden!")
            return
        }
        val type = germanTypeList.random()
        val mon = d.picks(d.current).random()
        d.replyGeneral(
            "die Terakristallisierung gegambled und den Typen `$type` auf ${mon.name} (${mon.tier}) bekommen!"
        )
        d.afterPickOfficial()
    }
}
