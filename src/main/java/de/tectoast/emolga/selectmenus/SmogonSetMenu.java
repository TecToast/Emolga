package de.tectoast.emolga.selectmenus;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

public class SmogonSetMenu extends MenuListener {

    public SmogonSetMenu() {
        super("smogonset");
    }

    @Override
    public void process(SelectionMenuEvent e) {
        SmogonSet smogon = Command.smogonMenu.get(e.getMessageIdLong());
        if (smogon == null) {
            e.reply("Dieses Smogon Set funktioniert nicht mehr, da der Bot seit der Erstellung neugestartet wurde. Bitte ruf den Command nochmal auf :)").setEphemeral(true).queue();
            e.getChannel().deleteMessageById(e.getMessageId()).queue();
            return;
        }
        smogon.changeSet(e.getInteraction().getValues().get(0));
        e.editMessage(smogon.buildMessage()).setActionRows(smogon.buildActionRows()).queue();
    }
}
