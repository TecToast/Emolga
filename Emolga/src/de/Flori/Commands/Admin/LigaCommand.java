package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;

public class LigaCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        Member member = e.getMember();
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        if (m.getMentionedRoles().size() == 0) {
            tco.sendMessage(member.getAsMention() + " Du musst eine Rolle angeben!").queue();
            return;
        }
        Guild g = tco.getGuild();
        Role r = m.getMentionedRoles().get(0);
        g.loadMembers().onSuccess((members) -> {
            ArrayList<Member> list = new ArrayList<>();
            for (Member mem : members) {
                if (mem.getRoles().contains(r))
                    list.add(mem);
            }
            if (list.size() % 2 != 0) {
                list.add(null);
            }
            int numTeams = list.size();
            int numDays = (numTeams - 1);
            int halfSize = numTeams / 2;
            ArrayList<Member> teams = new ArrayList<>(list);
            teams.remove(0);
            int teamsSize = teams.size();
            StringBuilder s = new StringBuilder();
            for (int day = 0; day < numDays; day++) {
                s.append("**Spieltag ").append(day + 1).append(":**\n");
                int teamIdx = day % teamsSize;
                s.append(teams.get(teamIdx) == null ? "(Platzhalter)" : teams.get(teamIdx).getEffectiveName()).append(" vs ")
                        .append(list.get(0) == null ? "(Platzhalter)" : list.get(0).getEffectiveName()).append("\n");
                for (int idx = 1; idx < halfSize; idx++) {
                    int firstTeam = (day + idx) % teamsSize;
                    int secondTeam = (day + teamsSize - idx) % teamsSize;
                    //System.out.println(teams.get(firstTeam) + "   " + teams.get(secondTeam));
                    s.append(teams.get(firstTeam) == null ? "(Platzhalter)" : teams.get(firstTeam).getEffectiveName())
                            .append(" vs ").append(teams.get(secondTeam) == null ? "(Platzhalter)" : teams.get(secondTeam).getEffectiveName()).append("\n");
                }
                s.append("\n");
            }
            tco.sendMessage(s.toString()).queue();
        });
    }

    public LigaCommand() {
        super("liga", "`!liga <Rolle>` Erstellt einen Spielplan mit allen Usern, die die angegebene Rolle besitzen", CommandCategory.Admin);
    }
}
