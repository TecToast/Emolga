package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.io.File
import java.util.*

class ShinyCommand : Command("shiny", "Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "regform", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true
            )
            .addEngl("mon", "Pokemon", "Das Mon, von dem das Shiny angezeigt werden soll", Translation.Type.POKEMON)
            .add("form", "Form", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!shiny Primarina")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        var suffix: String
        val args = e.arguments!!
        val monname = e.arguments!!.getTranslation("mon").translation
        val mon = dataJSON.getJSONObject(toSDName(monname))
        suffix = if (args.has("regform")) {
            val form = args.getText("regform")
            "-" + form.lowercase(Locale.getDefault())
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
            if (otherFormes.toList().stream().noneMatch { s: Any ->
                    (s as String).lowercase(Locale.getDefault()).endsWith("-" + form.lowercase(Locale.getDefault()))
                }) {
                e.reply("$monname besitzt keine **$form**-Form!")
                return
            }
            if (suffix.isEmpty()) suffix = "-"
            suffix += form.lowercase(Locale.getDefault())
        }
        val f =
            File("../Showdown/sspclient/sprites/gen5-shiny/" + monname.lowercase(Locale.getDefault()) + suffix + ".png")
        if (!f.exists()) {
            e.reply(mon.toString() + " hat keine " + args.getText("form") + "-Form!")
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.textChannel.sendFile(f).queue()
    }
}