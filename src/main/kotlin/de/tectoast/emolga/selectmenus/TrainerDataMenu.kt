package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent

class TrainerDataMenu : MenuListener("trainerdata") {
    override fun process(e: SelectMenuInteractionEvent, menuname: String?) {
        val dt = Command.trainerDataButtons[e.messageIdLong]
        if (dt == null) {
            e.editMessage_(
                embed = Embed(
                    title = "Diese Trainer-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!",
                    color = embedColor
                )
            ).queue()
            return
        }
        val withMoveset = dt.isWithMoveset
        val name = e.values[0]
        dt.current = name
        e.editMessage_(
            embed = Embed(
                title = dt.getNormalName(name),
                description = dt.getMonsFrom(name, withMoveset),
                color = embedColor
            ),
            components = Command.getTrainerDataActionRow(dt, withMoveset)
        ).queue()
    }
}