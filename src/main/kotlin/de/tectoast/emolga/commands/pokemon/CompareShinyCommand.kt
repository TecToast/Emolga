package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File

class CompareShinyCommand : Command(
    "compareshiny",
    "Zeigt den normalen Sprite und den Shiny-Sprite eines Pokémon an",
    CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "regform", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true
            )
            .addEngl("mon", "Pokemon", "Das Mon, von dem das Shiny angezeigt werden soll", Translation.Type.POKEMON)
            .add("form", "Form", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!compareshiny Primarina")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        var suffix: String
        val args = e.arguments
        val monname = args.getTranslation("mon").translation
        val mon = dataJSON.getJSONObject(toSDName(monname))
        suffix = if (args.has("regform")) {
            val form = args.getText("regform")
            "-" + form.lowercase()
        } else {
            ""
        }
        if (args.has("form")) {
            val form = args.getText("form")
            if (!mon.has("otherFormes")) {
                e.reply("$monname besitzt keine **$form**-Form!")
                return
            }
            val otherFormes = mon.getJSONArray("otherFormes")
            if (otherFormes.toStringList().none {
                    it.lowercase().endsWith("-" + form.lowercase())
                }) {
                e.reply("$monname besitzt keine **$form**-Form!")
                return
            }
            if (suffix.isEmpty()) suffix = "-"
            suffix += form.lowercase()
        }
        val fn = File("../Showdown/sspclient/sprites/gen5/" + monname.lowercase() + suffix + ".png")
        val fs =
            File("../Showdown/sspclient/sprites/gen5-shiny/" + monname.lowercase() + suffix + ".png")
        if (!fn.exists()) {
            e.reply(mon.toString() + " hat keine " + args.getText("form") + "-Form!")
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.textChannel.sendFiles(FileUpload.fromData(fn), FileUpload.fromData(fs)).queue()
    }
}