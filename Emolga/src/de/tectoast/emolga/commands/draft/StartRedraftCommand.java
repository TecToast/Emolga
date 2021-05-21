package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;

public class StartRedraftCommand extends Command {

    public StartRedraftCommand() {
        super("startredraft", "Startet einen Redraft", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
                .add("tc", "Channel", "Der Channel, wo die Teamübersicht sein soll", ArgumentManagerTemplate.DiscordType.CHANNEL)
                .setExample("!startredraft Emolga-Conference #emolga-teamübersicht")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        new Draft(e.getChannel(), args.getText("name"), args.getChannel("tc").getId(), false, true);
    }
}
