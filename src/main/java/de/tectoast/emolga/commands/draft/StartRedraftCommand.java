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
                .add("file", "FromFile", "Obs von der File geladen werden soll `file`", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!startredraft Emolga-Conference")
                .build());
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L, 280825853401628674L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        boolean fromFile = args.getOrDefault("file", "").equalsIgnoreCase("file");
        new Draft(e.getChannel(), args.getText("name"), null, fromFile, true);
        if (fromFile) e.getMessage().delete().queue();
    }
}
