package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.*;

public class SpielplanCommand extends Command {
    public SpielplanCommand() {
        super("spielplan", "`!spielplan <Name> <TCID> <RID>` Erstellt einen Spielplan für diesen Draft im Channel TCID und pingt die Rolle RID", CommandCategory.Flo);
        this.disable();
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            Message mes = tco.sendMessage("Lädt...").complete();
            Guild g = tco.getGuild();
            String name = msg.split(" ")[1];
            String tcid = msg.split(" ")[2];
            String rid = msg.split(" ")[3];
            JSONObject json = getEmolgaJSON();
            JSONObject drafts = json.getJSONObject("drafts");
            if (!drafts.has(name)) {
                tco.sendMessage("Dieser Draft existiert nicht!").queue();
                return;
            }
            JSONObject league = drafts.getJSONObject(name);
            league.put("announcementchannel", tcid);
            league.put("role", rid);
            ArrayList<String> listTeams = new ArrayList<>(Arrays.asList(league.getJSONObject("order").getString("1").split(",")));
            league.put("battleorder", new JSONObject());
            JSONObject order = league.getJSONObject("battleorder");
            if (listTeams.size() % 2 != 0) {
                listTeams.add("Bye");
            }
            int numTeams = listTeams.size();
            int numDays = (numTeams - 1);
            int halfSize = numTeams / 2;
            ArrayList<String> teams = new ArrayList<>(listTeams);
            teams.remove(0);
            int teamsSize = teams.size();
            StringBuilder s = new StringBuilder();
            for (int day = 0; day < numDays; day++) {
                s.append("**Spieltag ").append(day + 1).append(":**\n");
                StringBuilder file = new StringBuilder();
                int teamIdx = day % teamsSize;
                file.append(teams.get(teamIdx)).append(":").append(listTeams.get(0)).append(";");
                s.append(g.retrieveMemberById(teams.get(teamIdx)).complete().getEffectiveName()).append(" vs ").append(g.retrieveMemberById(listTeams.get(0)).complete().getEffectiveName()).append("\n");
                for (int idx = 1; idx < halfSize; idx++) {
                    int firstTeam = (day + idx) % teamsSize;
                    int secondTeam = (day + teamsSize - idx) % teamsSize;
                    //System.out.println(teams.get(firstTeam) + "   " + teams.get(secondTeam));
                    file.append(teams.get(firstTeam)).append(":").append(teams.get(secondTeam)).append(";");
                    System.out.println(teams.get(firstTeam) + " " + teams.get(secondTeam));
                    s.append(g.retrieveMemberById(teams.get(firstTeam)).complete().getEffectiveName())
                            .append(" vs ").append(g.retrieveMemberById(teams.get(secondTeam)).complete().getEffectiveName()).append("\n");
                }
                order.put(Integer.toString(day + 1), file.toString());
                s.append("\n");
            }
            tco.getGuild().getTextChannelById(tcid).sendMessage(s.toString()).queue();
            Calendar c = Calendar.getInstance();
            league.put("announcements", new JSONObject());
            for (int i = 1; i <= numDays; i++) {
                while (((c.get(Calendar.MONTH) + 1) <= 7 && c.get(Calendar.DAY_OF_MONTH) < 27) || c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    c.add(Calendar.DATE, 1);
                }
                c.set(Calendar.HOUR_OF_DAY, 8);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                //System.out.println(new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss").format(c.getTime()));
                league.getJSONObject("announcements").put(String.valueOf(i), c.getTimeInMillis());
                int finalI = i;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        JSONObject json = getEmolgaJSON();
                        JSONObject league = json.getJSONObject("drafts").getJSONObject(name);
                        Guild g = EmolgaMain.emolgajda.getGuildById(league.getString("guild"));
                        TextChannel tc = g.getTextChannelById(league.getString("announcementchannel"));
                        StringBuilder s = new StringBuilder("**Spieltag " + finalI + ":**\n");
                        for (String order : league.getJSONObject("battleorder").getString(String.valueOf(finalI)).split(";")) {
                            s.append(g.retrieveMemberById(order.split(":")[0]).complete().getEffectiveName()).append(" vs ").append(g.retrieveMemberById(order.split(":")[1]).complete().getEffectiveName()).append("\n");
                        }
                        s.append(g.getRoleById(league.getString("role")).getAsMention());
                        tc.sendMessage(s.toString()).queue();
                    }
                }, c.getTimeInMillis() - System.currentTimeMillis());
                System.out.println(i + ": " + c.getTimeInMillis() + " " + (c.getTimeInMillis() - System.currentTimeMillis()));
                c.add(Calendar.DATE, 1);
            }
            saveEmolgaJSON();
            mes.delete().queue();
        } catch (Exception ex) {
            ex.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!\nSyntax: `!spielplan <Name der Liga> <TCID> <RID>`").queue();
        }
    }
}
