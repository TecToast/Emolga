package de.tectoast.commands.various;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class FloHelpCommand extends Command {
    public FloHelpCommand() {
        super("flohelp", "`!flohelp <Nachricht>` Nutzt diesen Command, falls irgendwelche Fehler auftreten sollen, um meinen Programmierer Flo zu benachrichtigen", CommandCategory.Various);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        sendToMe(e.getChannel().getAsMention() + " - " + e.getMember().getAsMention() + ":\n" + e.getMessage().getContentRaw().substring(9));
    }
}
