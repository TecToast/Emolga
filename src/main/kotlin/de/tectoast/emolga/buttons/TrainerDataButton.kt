package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class TrainerDataButton : ButtonListener("trainerdata") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val dt = Command.trainerDataButtons[e.messageIdLong]
        if (dt == null) {
            e.reply("Dieses Trainer-Data funktioniert nicht mehr, da der Bot seit der Erstellung neugestartet wurde. Bitte ruf den Command nochmal auf :)")
                .setEphemeral(true).queue()
            e.hook.deleteOriginal().queue()
            return
        }
        val withMoveset = dt.isWithMoveset
        if (name == "CHANGEMODE") {
            dt.swapWithMoveset()
            e.editComponents(Command.getTrainerDataActionRow(dt, !withMoveset)).queue()
            val title = e.message.embeds[0].title
            if (!title!!.contains("Und sollen die Moves etc auch angezeigt werden")) {
                e.hook.editOriginalEmbeds(
                    Embed(title = title, description = dt.getMonsFrom(title, !withMoveset))
                ).queue()
            }
        }
    }
}