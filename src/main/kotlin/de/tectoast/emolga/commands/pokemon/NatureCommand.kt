package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.NatureManager

class NatureCommand : Command("nature", "Zeigt an, welche Werte dieses Wesen beeinflusst", CommandCategory.Pokemon) {
    init {
        argumentTemplate =
            ArgumentManagerTemplate.builder().addEngl("nature", "Wesen", "Das Wesen", Translation.Type.NATURE)
                .setExample("!nature Adamant")
                .build()
    }

    override fun process(e: GuildCommandEvent) {
        val t = e.arguments!!.getTranslation("nature")
        e.reply(
            """
    ${t.otherLang}/${t.translation}:
    ${NatureManager.getNatureData(t.translation)}
    """.trimIndent()
        )
    }
}