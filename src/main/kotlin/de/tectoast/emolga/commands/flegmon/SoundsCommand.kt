package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import dev.minn.jda.ktx.messages.Embed
import java.awt.Color
import java.io.File

class SoundsCommand : PepeCommand("sounds", "Zeigt alle Sound-Snippets an, die der Bot hat") {
    private val off = listOf("scream", "screamlong", "rickroll")

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(
            Embed(
                title = "Sounds",
                color = Color.PINK.rgb,
                description = File("audio/clips/").listFiles()!!.asSequence().map { it.name.substringBefore(".") }
                    .filter { it !in off }.sorted().joinToString("\n")
            )
        )
    }
}