package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.Translation

class AllLearnCommand : Command(
    "alllearn",
    "Zeigt, welche der angegeben Pokemon die angegebene Attacke lernen können.",
    CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("move", "Attacke", "Die Attacke, nach der geschaut werden soll", Translation.Type.MOVE)
            .add("mons", "Pokemon", "Alle Pokemon, mit Leerzeichen separiert", ArgumentManagerTemplate.Text.any())
            .setExample("!alllearn Tarnsteine Regirock Primarene Bisaflor Humanolith")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val atk = args.getTranslation("move").translation
        val str = StringBuilder(2 shl 5)
        val mons = args.getText("mons")
        if (mons.contains("\n")) {
            for (s in mons.split("\n")) {
                str.append(
                    if (canLearn(
                            if (s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-")) s.substring(2) else s,
                            if (s.startsWith("A-")) "" else if (s.startsWith("G-")) "Galar" else "Normal",
                            atk,
                            e.msg ?: "",
                            if (e.guild.id == "747357029714231299") 5 else 8
                        )
                    ) """
     $s
     
     """.trimIndent() else ""
                )
            }
        } else {
            for (s in mons.split(" ")) {
                str.append(
                    if (canLearn(
                            if (s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-")) s.substring(2) else s,
                            if (s.startsWith("A-")) "" else if (s.startsWith("G-")) "Galar" else "Normal",
                            atk,
                            e.msg ?: "",
                            if (e.guild.id == "747357029714231299") 5 else 8
                        )
                    ) "$s\n" else ""
                )
            }
        }
        if (str.toString().isEmpty()) str.append("Kein Pokemon kann diese Attacke erlernen!")
        e.reply(str.toString())
    }
}
