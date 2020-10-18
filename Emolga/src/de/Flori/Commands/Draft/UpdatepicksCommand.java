package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Draft.DraftPokemon;
import de.Flori.utils.Draft.Tierlist;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UpdatepicksCommand extends Command {
    public UpdatepicksCommand() {
        super("updatepicks", "`!updatepicks <TID> <Name>", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String tid = msg.split(" ")[1];
            String name = msg.substring(tid.length() + 14);
            TextChannel tc = e.getJDA().getTextChannelById(tid);
            Guild g = tc.getGuild();
            JSONObject json = getEmolgaJSON();
            if (json.has("drafts")) {
                JSONObject drafts = json.getJSONObject("drafts").getJSONObject("ASLS7");
                if (drafts.has(name)) {
                    JSONObject league = drafts.getJSONObject(name);
                    ArrayList<Message> li = new ArrayList<>();
                    for (Message message : tc.getIterableHistory()) {
                        li.add(message);
                    }
                    Collections.reverse(li);
                    int i = 0;
                    for (String p : league.getJSONObject("order").getString("1").split(",")) {
                        ArrayList<DraftPokemon> list = league.getJSONObject("picks").getJSONArray(p).toList().stream().map(o -> {
                            HashMap<String, Object> obj = (HashMap<String, Object>) o;
                            return new DraftPokemon((String) obj.get("name"), (String) obj.get("tier"));
                        }).collect(Collectors.toCollection(ArrayList::new));
                        StringBuilder mes = new StringBuilder("**" + g.retrieveMemberById(p).complete().getEffectiveName() + ":**\n");
                        for (String o : Tierlist.getByGuild(g.getId()).order) {
                            ArrayList<DraftPokemon> mons = list.stream().filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(o2 -> o2.name)).collect(Collectors.toCollection(ArrayList::new));
                            for (DraftPokemon mon : mons) {
                                mes.append(o).append(": ").append(mon.name).append("\n");
                            }
                        }
                        li.get(i).editMessage(mes.toString()).queue();
                        //tco.sendMessage(mes.toString()).queue();
                        i++;
                    }
                    tco.sendMessage("Success!").queue();
                } else {
                    tco.sendMessage("Es gibt keine Liga mit dem Namen " + name + "!").queue();
                }
            } else tco.sendMessage("Es wurde noch kein Draft erstellt!").queue();
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }
}
