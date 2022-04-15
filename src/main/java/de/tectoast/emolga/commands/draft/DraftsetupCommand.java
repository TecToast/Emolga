package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;

public class DraftsetupCommand extends Command {
    public DraftsetupCommand() {
        super("draftsetup", "Startet das Draften der Liga in diesem Channel", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
                .add("tc", "Channel", "Der Channel, wo die Teamübersicht sein soll", ArgumentManagerTemplate.DiscordType.CHANNEL, true)
                .setExample("!draftsetup Emolga-Conference #emolga-teamübersicht")
                .build());
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        new Draft(e.getChannel(), args.getText("name"), args.has("tc") ? args.getChannel("tc").getId() : null, false);
    }
}
