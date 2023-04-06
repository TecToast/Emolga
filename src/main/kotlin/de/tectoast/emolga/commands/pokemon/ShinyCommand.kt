package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.Translation
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File

class ShinyCommand : Command("shiny", "Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "regform", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
            ), true
        ).addEngl("mon", "Pokemon", "Das Mon, von dem das Shiny angezeigt werden soll", Translation.Type.POKEMON)
            .add("form", "Form", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!shiny Primarina").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        var suffix: String
        val args = e.arguments
        val monname = args.getTranslation("mon").translation
        val mon = getDataObject(monname)
        suffix = if (args.has("regform")) {
            val form = args.getText("regform")
            "-" + form.lowercase()
        } else {
            ""
        }
        if (args.has("form")) {
            val form = args.getText("form")
            val otherFormes = mon.otherFormes ?: run {
                e.reply("$monname besitzt keine **$form**-Form!")
                return

            }
            if (otherFormes.none {
                    it.lowercase().endsWith("-" + form.lowercase())
                }) {
                e.reply("$monname besitzt keine **$form**-Form!")
                return
            }
            if (suffix.isEmpty()) suffix = "-"
            suffix += form.lowercase()
        }
        val f = File("../Showdown/sspclient/sprites/gen5-shiny/" + monname.lowercase() + suffix + ".png")
        if (!f.exists()) {
            e.reply(mon.toString() + " hat keine " + args.getText("form") + "-Form!")
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.textChannel.sendFiles(FileUpload.fromData(f)).queue()
    }
}
