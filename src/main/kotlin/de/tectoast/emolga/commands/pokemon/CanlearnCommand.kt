package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class CanlearnCommand :
    Command("canlearn", "Zeigt, ob das Pokemon diese Attacke erlernen kann", CommandCategory.Pokemon) {
    init {
        aliases.add("canlearn5")
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true
            )
            .add("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
            .add("move", "Attacke", "Die Attacke", Translation.Type.MOVE)
            .setExample("!canlearn Alola Vulnona Ice Beam")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        val pokemon = args.getTranslation("mon").translation
        val atk = args.getTranslation("move").translation
        val form = args.getOrDefault("form", "Normal")
        try {
            e.reply(
                (if (form == "Normal") "" else "$form-") + pokemon + " kann " + atk + (if (canLearn(
                        pokemon,
                        form,
                        atk,
                        e.msg ?: "",
                        if (e.guild.id == "747357029714231299" || e.usedName.equals(
                                "canlearn5",
                                ignoreCase = true
                            )
                        ) 5 else 8
                    )
                ) "" else " nicht") + " erlernen!"
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}