package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL

object TeraMDLCommand : TestableCommand<NoCommandArgs>("teramdl", "Randomized den Tera-Typen") {


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

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoCommandArgs

    context (InteractionData)
    override suspend fun exec(e: NoCommandArgs) {
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
        val type = typeList.random()
        val mon = d.picks(d.current).random()
        d.replyGeneral(
            "die Terakristallisierung gegambled und den Typen `$type` auf ${mon.name} (${mon.tier}) bekommen!"
        )
        d.afterPickOfficial()
    }
}
