package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;

public class StopdraftCommand extends Command {
    public StopdraftCommand() {
        super("stopdraft", "Beendet den Draft", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("draftname", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.draft())
                .setExample("!stopdraft Emolga-Conference")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        if (Draft.drafts.removeIf(d -> {
            if (d.name.equals(e.getArguments().getText("draftname"))) {
                d.cooldown.cancel(false);
                return true;
            }
            return false;
        })) {
            e.reply("Dieser Draft wurde beendet!");
        } else {
            e.reply("Dieser Draft existiert nicht!");
        }
    }
}
