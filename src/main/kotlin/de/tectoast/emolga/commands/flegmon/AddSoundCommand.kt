package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import java.io.File

class AddSoundCommand : PepeCommand("addsound", "Added einen Sound") {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "sound",
                "Sound",
                "Der Sound, der hinzugefügt werden soll",
                ArgumentManagerTemplate.DiscordFile.of("mp3")
            )
            .setExample("!addsound <Hier Sound-Datei einfügen>")
            .build()
        wip()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val a = e.arguments.getAttachment("sound")
        val fileName = a.fileName
        val f = File("audio/clips/$fileName")
        if (f.exists()) {
            e.reply("Ein Sound mit dem Namen " + fileName.substring(0, fileName.length - 4) + " gibt es bereits!")
            return
        }
        a.proxy.downloadToFile(f).thenAccept { e.reply("Der Sound wurde hinzugefügt!") }
    }
}