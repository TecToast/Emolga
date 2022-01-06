package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;

public class LigaCommand extends Command {
    public LigaCommand() {
        super("liga", "Erstellt einen Spielplan mit allen Usern, die die angegebene Rolle besitzen", CommandCategory.Admin);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("role", "Rolle", "Die Rolle, die verwendet werden soll", ArgumentManagerTemplate.DiscordType.ROLE)
                .setExample("!liga @S1-Teilnehmer")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member member = e.getMember();
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        Guild g = tco.getGuild();
        Role r = e.getArguments().getRole("role");
        g.findMembers(mem -> mem.getRoles().contains(r)).onSuccess((members) -> {
            if (members.size() % 2 != 0) {
                members.add(null);
            }
            int numTeams = members.size();
            int numDays = (numTeams - 1);
            int halfSize = numTeams / 2;
            ArrayList<Member> teams = new ArrayList<>(members);
            teams.remove(0);
            int teamsSize = teams.size();
            StringBuilder s = new StringBuilder();
            for (int day = 0; day < numDays; day++) {
                s.append("**Spieltag ").append(day + 1).append(":**\n");
                int teamIdx = day % teamsSize;
                s.append(teams.get(teamIdx) == null ? "(Platzhalter)" : teams.get(teamIdx).getEffectiveName()).append(" vs ")
                        .append(members.get(0) == null ? "(Platzhalter)" : members.get(0).getEffectiveName()).append("\n");
                for (int idx = 1; idx < halfSize; idx++) {
                    int firstTeam = (day + idx) % teamsSize;
                    int secondTeam = (day + teamsSize - idx) % teamsSize;
                    //logger.info(teams.get(firstTeam) + "   " + teams.get(secondTeam));
                    s.append(teams.get(firstTeam) == null ? "(Platzhalter)" : teams.get(firstTeam).getEffectiveName())
                            .append(" vs ").append(teams.get(secondTeam) == null ? "(Platzhalter)" : teams.get(secondTeam).getEffectiveName()).append("\n");
                }
                s.append("\n");
            }
            tco.sendMessage(s.toString()).queue();
        });
    }
}
