package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.io.File
import java.util.*
import java.util.stream.Collectors

class SoundsCommand : PepeCommand("sounds", "Zeigt alle Sound-Snippets an, die der Bot hat") {
    private val off = listOf("scream", "screamlong", "rickroll")

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        e.reply(
            EmbedBuilder().setTitle("Sounds").setColor(Color.PINK)
                .setDescription(Arrays.stream(File("audio/clips/").listFiles()).map { file: File ->
                    file.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]
                }.filter { s: String -> !off.contains(s) }.sorted().collect(Collectors.joining("\n"))).build()
        )
    }
}