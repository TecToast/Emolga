package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Draft.Draft;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Optional;

public class RoundCommand extends Command {
    public RoundCommand() {
        super("round", "`!round` Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Optional<Draft> op = Draft.drafts.stream().filter(d -> d.tc.getId().equals(tco.getId())).findFirst();
        if (!op.isPresent()) {
            tco.sendMessage("In diesem Textchannel findet derzeit kein draft statt!").queue();
            return;
        }
        tco.sendMessage("Der draft ist in Runde " + op.get().round + "!").queue();
    }
}
