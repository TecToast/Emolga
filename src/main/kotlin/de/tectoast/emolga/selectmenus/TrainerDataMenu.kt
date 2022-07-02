package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import java.awt.Color

class TrainerDataMenu : MenuListener("trainerdata") {
    override fun process(e: SelectMenuInteractionEvent, menuname: String?) {
        val dt = Command.trainerDataButtons[e.messageIdLong]
        if (dt == null) {
            e.editMessageEmbeds(
                EmbedBuilder().setTitle("Ach Mensch " + e.member!!.effectiveName + ", diese Trainer-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!")
                    .setColor(
                        Color.CYAN
                    ).build()
            ).queue()
            return
        }
        val withMoveset = dt.isWithMoveset
        val name = e.values[0]
        dt.current = name
        e.editMessageEmbeds(
            EmbedBuilder().setColor(Color.CYAN).setTitle(dt.getNormalName(name))
                .setDescription(dt.getMonsFrom(name, withMoveset)).build()
        ).setActionRows(
            Command.getTrainerDataActionRow(dt, withMoveset)
        ).queue()
    }
}