package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Optional;

public class RoundCommand extends Command {
    public RoundCommand() {
        super("round", "`!round` Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Optional<Draft> op = Draft.drafts.stream().filter(d -> d.tc.getId().equals(tco.getId())).findFirst();
        if (!op.isPresent()) {
            tco.sendMessage("In diesem Textchannel findet derzeit kein draft statt!").queue();
            return;
        }
        tco.sendMessage("Der draft ist in Runde " + op.get().round + "!").queue();
    }
}
