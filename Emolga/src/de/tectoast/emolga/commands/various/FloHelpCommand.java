package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class FloHelpCommand extends Command {
    public FloHelpCommand() {
        super("flohelp", "`!flohelp <Nachricht>` Nutzt diesen Command, falls irgendwelche Fehler auftreten sollen, um meinen Programmierer Flo zu benachrichtigen", CommandCategory.Various);
    }

    @Override
    public void process(GuildCommandEvent e) {
        sendToMe(e.getChannel().getAsMention() + " - " + e.getMember().getAsMention() + ":\n" + e.getMessage().getContentRaw().substring(9));
    }
}
