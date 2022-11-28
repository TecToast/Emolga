package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

class SmogonFormatMenu : MenuListener("smogonformat") {
    override fun process(e: StringSelectInteractionEvent, menuname: String?) {
        val smogon = Command.smogonMenu[menuname!!]
        if (smogon == null) {
            e.reply("Dieses Smogon Set funktioniert nicht mehr, da der Bot seit der Erstellung neugestartet wurde. Bitte ruf den Command nochmal auf :)")
                .setEphemeral(true).queue()
            e.channel.deleteMessageById(e.messageId).queue()
            return
        }
        smogon.changeFormat(e.values[0])
        e.editMessage(smogon.buildMessage()).setComponents(smogon.buildActionRows(menuname)).queue()
    }
}
