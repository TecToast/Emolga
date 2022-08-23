package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File

class AprilFoolsSpriteCommand : Command("aprilfoolsprite", "Zeigt den April-Fools-Sprite", CommandCategory.Pokemon) {
    init {
        aliases.add("afd")
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "back", "Backsprite", "", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Front", "Wenn der Front-Sprite des Mons angezeigt werden soll (Standart)"),
                    SubCommand.of("Back", "Wenn der Back-Sprite des Mons angezeigt werden soll")
                ), true
            )
            .add(
                "shiny",
                "Shiny",
                "",
                ArgumentManagerTemplate.Text.of(
                    SubCommand.of(
                        "Shiny",
                        "Wenn der Sprite des Mons als Shiny angezeigt werden soll"
                    )
                ),
                true
            )
            .add(
                "form", "Form", "", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true
            )
            .addEngl("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
            .setExample("!afd Galar Zigzachs")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val suffix: String
        val args = e.arguments
        suffix = if (args.has("form")) {
            val form = args.getText("form")
            "-" + form.lowercase()
        } else {
            ""
        }
        val mon = args.getTranslation("mon").translation
        if (mon == "Popplio" || mon == "Primarina") {
            e.reply("Möchte da jemand gebannt werden? :^)")
            return
        }
        val f = File(
            "../Showdown/sspclient/sprites/afd"
                    + (if (args.isText("back", "Back")) "-back" else "")
                    + (if (args.isText("shiny", "Shiny")) "-shiny" else "")
                    + "/" + mon.lowercase() + suffix + ".png"
        )
        if (!f.exists()) {
            e.reply(mon + " hat keine " + args.getText("form") + "-Form!")
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.textChannel.sendFiles(FileUpload.fromData(f)).queue()
    }
}