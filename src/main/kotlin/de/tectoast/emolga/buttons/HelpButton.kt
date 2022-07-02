package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.awt.Color
import java.util.*

class HelpButton : ButtonListener("help") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val g = e.guild!!
        val mem = e.member!!
        val c = CommandCategory.byName(name)!!
        if (c.allowsGuild(g) && c.allowsMember(mem)) {
            val l = Command.getWithCategory(c, g, mem)
            val embeds: MutableList<EmbedBuilder> = LinkedList()
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
            embeds[embeds.size - 1].setFooter("<Benötigtes Argument>          [Optionales Argument]")
            e.editMessageEmbeds(embeds.stream().map { obj: EmbedBuilder -> obj.build() }
                .toList())
                .queue { i: InteractionHook -> i.editOriginalComponents(Command.getHelpButtons(g, mem)).queue() }
        } else {
            e.reply("Auf die Kategorie " + c.categoryName + " hast du keinen Zugriff!").queue()
        }
    }
}