package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;

import java.util.Optional;

public class RoundCommand extends Command {
    public RoundCommand() {
        super("round", "Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Optional<Draft> op = Draft.drafts.stream().filter(d -> d.tc.getId().equals(e.getChannel().getId())).findFirst();
        if (op.isEmpty()) {
            e.reply("In diesem Textchannel findet derzeit kein Draft statt!");
            return;
        }
        e.reply("Der Draft ist in Runde " + op.get().round + "!");
    }
}
