package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.awt.Color

class TrainerDataButton : ButtonListener("trainerdata") {
    override fun process(e: ButtonInteractionEvent, name: String) {
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
                    EmbedBuilder().setColor(Color.CYAN).setTitle(title).setDescription(
                        dt.getMonsFrom(
                            title, !withMoveset
                        )
                    ).build()
                ).queue()
            }
            //return;
        }
        //e.editMessageEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(dt.getNormalName(name)).setDescription(dt.getMonsFrom(name, withMoveset)).build()).queue();
    }
}