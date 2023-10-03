package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.invoke
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL

object TeraMDLCommand : Command("teramdl", "Randomized den Tera-Typen", CommandCategory.Draft) {


    val typeList = setOf(
            "Normal",
            "Feuer",
            "Wasser",
            "Pflanze",
            "Gestein",
            "Boden",
            "Geist",
            "Unlicht",
            "Drache",
            "Fee",
            "Eis",
            "Kampf",
            "Elektro",
            "Flug",
            "Gift",
            "Psycho",
            "Stahl",
            "Käfer"
        )


    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(false, Constants.G.VIP)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (d !is MDL) {
            e.reply("Dieser Befehl funktioniert nur im MDL Draft!")
            return
        }
        if (!d.isLastRound) {
            e.reply("Dieser Befehl kann nur in der letzten Runde verwendet werden!")
            return
        }
        val type = typeList.random()
        val mon = d.picks(d.current).random()
        d.replyGeneral(
            e,
            "die Terakristallisierung gegambled und den Typen `$type` auf ${mon.name} (${mon.tier}) bekommen!"
        )
        d.afterPickOfficial()
    }
}
