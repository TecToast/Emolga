package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class CreatedraftCommand extends Command {
    public CreatedraftCommand() {
        super("createdraft", "Generiert eine Draftreihenfolge mit allen Usern, die die Rolle haben", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Name", "Name des Drafts/der Liga", ArgumentManagerTemplate.Text.any())
                .add("role", "Rolle", "Rolle, von der alle User in die Draftreihenfolge kommen", ArgumentManagerTemplate.DiscordType.ROLE)
                .setExample("!createdraft Emolga-Conference @Emolga-Teilnehmer")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String name = args.getText("name");
        e.getGuild().findMembers(mem -> mem.getRoles().contains(args.getRole("role"))).onSuccess(members -> {
            if (members.size() == 0) {
                e.reply("Niemand hat diese Rolle!");
                return;
            }
            ArrayList<Member> order = new ArrayList<>();
            ArrayList<Member> invertedOrder = new ArrayList<>(order);
            Collections.reverse(invertedOrder);
            HashMap<Integer, ArrayList<Member>> map = new HashMap<>();
            for (int i = 1; i <= 13; i++) {
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
            for (int i = 1; i <= 13; i++) {
                builder.append(i).append(". Runde\n").append(map.get(i).stream().map(Member::getEffectiveName).collect(Collectors.joining("\n"))).append("\n\n");
            }
            e.replyMessage(builder.toString()).thenAccept(message -> message.pin().queue());
            JSONObject json = getEmolgaJSON();
            if (!json.has("drafts")) json.put("drafts", new JSONObject());
            JSONObject drafts = json.getJSONObject("drafts");
            HashMap<Integer, String> jstring = new HashMap<>();
            for (int i = 1; i <= 13; i++) {
                jstring.put(i, map.get(i).stream().map(Member::getId).collect(Collectors.joining(",")));
            }
            JSONObject o = new JSONObject();
            o.put("order", jstring);
            o.put("guild", e.getGuild().getId());
            drafts.put(name, o);
            saveEmolgaJSON();
        });
    }
}
