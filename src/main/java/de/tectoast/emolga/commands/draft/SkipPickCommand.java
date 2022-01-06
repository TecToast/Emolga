package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class SkipPickCommand extends Command {
    public SkipPickCommand() {
        super("skippick", "Skippe eine Person beim Draft", CommandCategory.Draft);
        setCustomPermissions(m -> m.hasPermission(Permission.ADMINISTRATOR) || m.getRoles().stream().anyMatch(r -> r.getId().equals("702233714360582154")));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tc = e.getChannel();
        Draft d = Draft.getDraftByChannel(tc);
        if (d != null) {
            d.timer(Draft.TimerReason.SKIP);
        }
    }
}
