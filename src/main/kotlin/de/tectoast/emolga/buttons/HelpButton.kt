package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import dev.minn.jda.ktx.messages.editMessage_
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.awt.Color

class HelpButton : ButtonListener("help") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val g = e.guild!!
        val mem = e.member!!
        val c = CommandCategory.byName(name)!!
        if (c.allowsGuild(g) && c.allowsMember(mem)) {
            val l = Command.getWithCategory(c, g, mem)
            val embeds: MutableList<EmbedBuilder> = mutableListOf()
            val b = StringBuilder()
            var first = true
            for (cmd in l) {
                b.append(cmd.getHelp(g)).append("\n")
                if (b.length > 1900) {
                    val emb = EmbedBuilder()
                    if (first) emb.setTitle(c.categoryName)
                    embeds.add(emb.setColor(Color.CYAN).setDescription(b.toString()))
                    first = false
                    b.setLength(0)
                }
            }
            if (b.isNotEmpty()) {
                val emb = EmbedBuilder()
                if (first) emb.setTitle(c.categoryName)
                embeds.add(emb.setColor(Color.CYAN).setDescription(b.toString()))
            }
            embeds.last().setFooter("<Benötigtes Argument>          [Optionales Argument]")
            e.editMessage_(embeds = embeds.map { it.build() }, components = Command.getHelpButtons(g, mem)).queue()
        } else {
            e.reply("Auf die Kategorie ${c.categoryName} hast du keinen Zugriff!").queue()
        }
    }
}