package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;

public class SetupfromfileCommand extends Command {
    public SetupfromfileCommand() {
        super("setupfromfile", "Setzt einen Draft auf Basis einer Datei auf", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.Text.any())
                .add("tc", "Text-Channel", "Der Text-Channel, wo die Teams drin stehen", ArgumentManagerTemplate.DiscordType.CHANNEL, true)
                .setExample("!setupfromfile Emolga-Conference #emolga-teamübersicht")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.deleteMessage();
        ArgumentManager args = e.getArguments();
        new Draft(e.getChannel(), args.getText("name"), args.has("tc") ? args.getChannel("tc").getId() : null, true);
    }
}
