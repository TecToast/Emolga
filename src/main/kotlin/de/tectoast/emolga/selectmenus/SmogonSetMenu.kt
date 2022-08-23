package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent

class SmogonSetMenu : MenuListener("smogonset") {
    override fun process(e: SelectMenuInteractionEvent, menuname: String?) {
        val smogon = Command.smogonMenu[e.messageIdLong]
        if (smogon == null) {
            e.reply("Dieses Smogon Set funktioniert nicht mehr, da der Bot seit der Erstellung neugestartet wurde. Bitte ruf den Command nochmal auf :)")
                .setEphemeral(true).queue()
            e.channel.deleteMessageById(e.messageId).queue()
            return
        }
        smogon.changeSet(e.values[0])
        e.editMessage(smogon.buildMessage()).setComponents(smogon.buildActionRows()).queue()
    }
}