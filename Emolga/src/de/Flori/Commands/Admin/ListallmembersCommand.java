package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ListallmembersCommand extends Command {
    public ListallmembersCommand() {
        super("listallmembers", "`!listallmembers` Zeigt alle Mitglieder des Servers an", CommandCategory.Admin);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
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
