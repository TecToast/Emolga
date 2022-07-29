package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class OffendCommand : Command("offend", "Offended Leute", CommandCategory.Various, 940283708953473075L) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("person", "Person", "Die Person", ArgumentManagerTemplate.Text.any())
            .setExample("!offend Emre")
            .build()
    }

    @Throws(Exception::class)
    override fun process(e: GuildCommandEvent) {
        val person = e.arguments.getText("person")
        when (person.lowercase()) {
            "emre" -> e.reply("Emre stinkt :^)")
            "discus" -> e.reply("Tower heißt Turret auf Englisch c:")
            "tobi" -> e.reply("Tobi kann Farben am Geruch erkennen \uD83D\uDC43")
            "sven" -> e.reply("Scheinbar unoffendbar \uD83D\uDC40")
        }
    }
}