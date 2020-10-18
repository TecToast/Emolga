package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Draft.Draft;
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
            tco.sendMessage("In diesem Textchannel findet derzeit kein Draft statt!").queue();
            return;
        }
        tco.sendMessage("Der Draft ist in Runde " + op.get().round + "!").queue();
    }
}
