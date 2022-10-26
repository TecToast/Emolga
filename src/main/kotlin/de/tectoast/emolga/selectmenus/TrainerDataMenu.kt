package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

class TrainerDataMenu : MenuListener("trainerdata") {
    override fun process(e: StringSelectInteractionEvent, menuname: String?) {
        val dt = Command.trainerDataButtons[e.messageIdLong]
        if (dt == null) {
            e.editMessage_(
                embeds = Embed(
                    title = "Diese Trainer-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!",
                    color = embedColor
                ).into()
            ).queue()
            return
        }
        val withMoveset = dt.isWithMoveset
        val name = e.values[0]
        dt.current = name
        e.editMessage_(
            embeds = Embed(
                title = dt.getNormalName(name),
                description = dt.getMonsFrom(name, withMoveset),
                color = embedColor
            ).into(),
            components = Command.getTrainerDataActionRow(dt, withMoveset)
        ).queue()
    }
}
