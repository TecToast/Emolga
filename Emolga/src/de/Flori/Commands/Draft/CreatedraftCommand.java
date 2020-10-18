package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class CreatedraftCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        if(m.getMentionedRoles().size() != 1) {
            tco.sendMessage("Du musst eine Rolle angeben!").queue();
            return;
        }
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.substring(13, msg.indexOf("@") - 1);
        tco.getGuild().findMembers(mem -> mem.getRoles().contains(m.getMentionedRoles().get(0))).onSuccess(members -> {
            if(members.size() == 0) {
                tco.sendMessage("Niemand hat diese Rolle!").queue();
                return;
            }
            ArrayList<Member> order = new ArrayList<>();
            ArrayList<Member> invertedOrder = new ArrayList<>(order);
            Collections.reverse(invertedOrder);
            HashMap<Integer, ArrayList<Member>> map = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                if (i % 2 == 0) {
                    order.clear();
                    order.addAll(invertedOrder);
                } else {
                    ArrayList<Member> list = new ArrayList<>(members);
                    order.clear();
                    while (!list.isEmpty()) {
                        order.add(list.remove(new Random().nextInt(list.size())));
                    }
                    invertedOrder.clear();
                    invertedOrder.addAll(order);
                    Collections.reverse(invertedOrder);
                }
                map.put(i, new ArrayList<>(order));
                //System.out.println(i);
            }
            //System.out.println(map.toString());
            StringBuilder builder = new StringBuilder(name + ":\n");
            for (int i = 1; i <= 12; i++) {
                builder.append(i).append(". Runde\n").append(map.get(i).stream().map(Member::getEffectiveName).collect(Collectors.joining("\n"))).append("\n\n");
            }
            tco.sendMessage(builder).complete().pin().queue();
            JSONObject json = getEmolgaJSON();
            if (!json.has("drafts")) json.put("drafts", new JSONObject());
            JSONObject drafts = json.getJSONObject("drafts");
            HashMap<Integer, String> jstring = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                jstring.put(i, map.get(i).stream().map(Member::getId).collect(Collectors.joining(",")));
            }
            JSONObject o = new JSONObject();
            o.put("order", jstring);
            o.put("guild", tco.getGuild().getId());
            drafts.put(name, o);
            saveEmolgaJSON();
        });
    }

    public CreatedraftCommand() {
        super("createdraft", "`!createdraft <Name> <Rolle>` Erstellt einen Draft mit dem Namen und generiert die Draftreihenfolge mit allen Usern, die die Rolle haben", CommandCategory.Draft, true);
    }
}
