package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class ListallmembersCommand extends Command {
    public ListallmembersCommand() {
        super("listallmembers", "Zeigt alle Mitglieder des Servers an", CommandCategory.Admin);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        tco.getGuild().loadMembers().onSuccess(list -> {
            StringBuilder s = new StringBuilder();
            for (Member mem : list) {
                s.append(mem.getEffectiveName()).append("\n");
            }
            tco.sendMessage(s.toString()).queue();
        });
    }
}
