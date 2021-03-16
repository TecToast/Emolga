package de.tectoast.emolga.commands;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.PrivateCommand;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.tectoast.emolga.commands.Command.*;

public class PrivateCommands {

    @PrivateCommand(name = "updatetierlist")
    public static void updateTierlist(JDA jda, MessageChannel tco, Message message) {
        Tierlist.setup();
        tco.sendMessage("Die Tierliste wurde aktualisiert!").queue();
    }

    @PrivateCommand(name = "skip")
    public static void skip(JDA jda, MessageChannel tco, Message message) {
        Draft.drafts.stream().filter(draft -> draft.name.equals(message.getContentDisplay().substring(6))).collect(Collectors.toList()).get(0).timer();
    }

    @PrivateCommand(name = "timer")
    public static void timer(JDA jda, MessageChannel tco, Message message) {
        String name = message.getContentDisplay().substring(7);
        List<Draft> list = Draft.drafts.stream().filter(d -> d.name.equals(name)).collect(Collectors.toList());
        if (list.size() == 0) {
            tco.sendMessage("Dieser draft existiert nicht!").queue();
            return;
        }
        Draft d = list.get(0);
        d.cooldown = new Timer();
        long delay = calculateASLTimer();
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                d.timer();
            }
        }, delay);
        getEmolgaJSON().getJSONObject("drafts").getJSONObject(name).put("timer", true);
        saveEmolgaJSON();
        tco.sendMessage("Der Timer bei " + name + " wurde aktiviert!").queue();
    }

    @PrivateCommand(name = "edit")
    public static void edit(JDA jda, MessageChannel tco, Message message) {
        String[] split = message.getContentDisplay().split(" ");
        jda.getTextChannelById(split[1]).editMessageById(split[2], message.getContentDisplay().substring(43)).queue();
    }

    @PrivateCommand(name = "send")
    public static void send(JDA jda, MessageChannel tco, Message message) {
        String[] split = message.getContentDisplay().split(" ");
        System.out.println(message.getContentRaw());
        String s = message.getContentRaw().substring(24).replaceAll("\\\\", "");
        TextChannel tc = jda.getTextChannelById(split[1]);
        Guild g = tc.getGuild();
        for (ListedEmote emote : g.retrieveEmotes().complete()) {
            s = s.replace("<<" + emote.getName() + ">>", emote.getAsMention());
        }
        tc.sendMessage(s).queue();
    }

    @PrivateCommand(name = "react")
    public static void react(JDA jda, MessageChannel tco, Message message) {
        String msg = message.getContentDisplay();
        String[] split = msg.split(" ");
        String s = msg.substring(45);
        TextChannel tc = jda.getTextChannelById(split[1]);
        Message m = tc.retrieveMessageById(split[2]).complete();
        assert (m != null);
        if (s.contains("<")) {
            s = s.substring(1);
            System.out.println("s = " + s);
            String finalS = s;
            tc.getGuild().retrieveEmotes().complete().stream().filter(emote -> emote.getName().equalsIgnoreCase(finalS)).forEach(emote -> m.addReaction(emote).queue());
        } else {
            m.addReaction(s).queue();
        }
    }

    @PrivateCommand(name = "ban")
    public static void ban(JDA jda, MessageChannel tco, Message message) {
        jda.getGuildById(message.getContentDisplay().split(" ")[1]).ban(message.getContentDisplay().split(" ")[2], 0).queue();
    }

    @PrivateCommand(name = "updatedatabase")
    public static void updateDatabase(JDA jda, MessageChannel tco, Message message) {
        loadJSONFiles();
        tco.sendMessage("Done!").queue();
    }

    @PrivateCommand(name = "emolgajson", aliases = {"ej"})
    public static void emolgajson(JDA jda, MessageChannel tco, Message message) {
        emolgajson = load("./emolgadata.json");
        tco.sendMessage("Done!").queue();
    }

    @PrivateCommand(name = "updategiveaways")
    public static void updateGiveaways(JDA jda, MessageChannel tco, Message message) {
        Giveaway.giveaways.clear();
        if (emolgajson.has("giveaways")) {
            JSONArray arr = emolgajson.getJSONArray("giveaways");
            for (Object o : arr) {
                JSONObject obj = (JSONObject) o;
                new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"), obj.has("role"));
            }
        }
        tco.sendMessage("Done!").queue();
    }

    @PrivateCommand(name = "sortbst")
    public static void sortBSTCmd(JDA jda, MessageChannel tco, Message message) {
        sortBST();
        tco.sendMessage("Done!").queue();
    }

    @PrivateCommand(name = "del")
    public static void del(JDA jda, MessageChannel tco, Message message) {
        jda.getTextChannelById(message.getContentDisplay().split(" ")[1]).deleteMessageById(message.getContentDisplay().split(" ")[2]).queue();
    }

    @PrivateCommand(name = "sortzbs")
    public static void sortZBSCmd(JDA jda, MessageChannel tco, Message message) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + message.getContentDisplay().split(" ")[1]);
        sortZBS(league.getString("sid"), "Liga " + message.getContentDisplay().split(" ")[1], league);
    }

    @PrivateCommand(name = "sortwooloo")
    public static void sortWoolooCmd(JDA jda, MessageChannel tco, Message message) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup");
        sortWooloo(league.getJSONObject("doc").getString("sid"), league);
    }

    @PrivateCommand(name = "troll")
    public static void troll(JDA jda, MessageChannel tco, Message message) {
        String[] split = message.getContentDisplay().split(" ");
        Category category = jda.getCategoryById(split[1]);
        Guild g = category.getGuild();
        Member user = g.retrieveMemberById(split[2]).complete();
        ArrayList<VoiceChannel> list = new ArrayList<>(category.getVoiceChannels());
        Collections.shuffle(list);
        VoiceChannel old = user.getVoiceState().getChannel();
        list.remove(old);
        for (VoiceChannel voiceChannel : list) {
            g.moveVoiceMember(user, voiceChannel).queue();
            try {
                Thread.sleep(500);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        g.moveVoiceMember(user, old).queue();
    }

    @PrivateCommand(name = "updatetable")
    public static void updateTableCmd(JDA jda, MessageChannel tco, Message message) {
        updateTable(getEmolgaJSON().getJSONObject("BlitzTurnier"), jda.getTextChannelById("771403849029386270"));
    }

    @PrivateCommand(name = "nextround")
    public static void nextRoundCmd(JDA jda, MessageChannel tco, Message message) {
        new Thread(() -> {
            JSONObject b = getEmolgaJSON().getJSONObject("BlitzTurnier");
            JSONObject bo = b.getJSONObject("battleorder");
            ArrayList<String> already = bo.keySet().stream().map(bo::getString).flatMap(s -> Arrays.stream(s.split(";"))).collect(Collectors.toCollection(ArrayList::new));
            ArrayList<String> order = new ArrayList<>();
            ArrayList<String> names = getBlitzTable(true).stream().map(l -> (String) l.get(0)).collect(Collectors.toCollection(ArrayList::new));
            for (int i = 0; i < names.size(); i++) {
                String s = names.get(i);
                if (order.stream().anyMatch(str -> str.contains(s))) continue;
                for (int j = i + 1; j < names.size(); j++) {
                    String str = names.get(j);
                    if (already.contains(str + ":" + s) || already.contains(s + ":" + str) || order.stream().anyMatch(string -> string.contains(str)))
                        continue;
                    order.add(s + ":" + str);
                    break;
                }
            }
            HashMap<String, String> namesmap = new HashMap<>();
            EmolgaMain.jda.getGuildById(Constants.BSID).retrieveMembersByIds(names.toArray(new String[0])).get().forEach(mem -> namesmap.put(mem.getId(), mem.getEffectiveName()));
            StringBuilder str = new StringBuilder();
            for (String s : order) {
                str.append(namesmap.get(s.split(":")[0])).append(" vs ").append(namesmap.get(s.split(":")[1])).append("\n");
            }
            tco.sendMessage(str.toString()).queue();
            b.put("gameday", b.getInt("gameday") + 1);
            bo.put(String.valueOf(b.getInt("gameday")), String.join(";", order));
            saveEmolgaJSON();
        }).start();
    }

    @PrivateCommand(name = "levels")
    public static void levels(JDA jda, MessageChannel tco, Message message) {
        new Thread(() -> {
            HashMap<String, String> map = new HashMap<>();
            JSONObject level = getLevelJSON();
            jda.getGuildById(Constants.BSID).retrieveMembersByIds(level.keySet().toArray(new String[0])).get().forEach(m -> map.put(m.getId(), m.getEffectiveName()));
            StringBuilder str = new StringBuilder();
            level.keySet().stream().sorted(Comparator.comparing(level::getInt).reversed()).forEach(s -> {
                if (map.get(s) == null) return;
                str.append(map.get(s)).append(": ").append(getLevelFromXP(level.getInt(s))).append("\n");
            });
            tco.sendMessage(str.toString()).queue();
        }).start();
    }

    @PrivateCommand(name = "generateorder")
    public static void generateOrder(JDA jda, MessageChannel tco, Message message) {
        new Thread(() -> {
            ArrayList<Member> list = new ArrayList<>(jda.getGuildById(Constants.BSID).findMembers(mem -> mem.getRoles().contains(jda.getGuildById(Constants.BSID).getRoleById("774659853812760587"))).get());
            Collections.shuffle(list);
            StringBuilder str = new StringBuilder();
            boolean b = false;
            for (Member member : list) {
                str.append(member.getId()).append(b ? ";" : ":");
                b = !b;
            }
            tco.sendMessage(str.toString()).queue();
        }).start();
    }

    @PrivateCommand(name = "deletedreplays")
    public static void deletedReplays(JDA jda, MessageChannel tco, Message message) {
        JSONObject a = getEmolgaJSON().getJSONObject("analyse");
        a.keySet().stream().filter(s -> jda.getTextChannelById(s) == null).collect(Collectors.toList()).forEach(a::remove);
        saveEmolgaJSON();
        updatePresence();
    }

    @PrivateCommand(name = "transferstuff")
    public static void transfer(JDA jda, MessageChannel tco, Message message) {
        JSONObject t = getWikiJSON().getJSONObject("translations");
        ArrayList<String> egg = new ArrayList<>();
        ArrayList<String> st = new ArrayList<>();
        for (String s : t.keySet()) {
            JSONObject o = t.getJSONObject(s);
            if(!o.getString("type").equals("egg")) continue;
            if(!s.equals(toSDName("e" + o.getString("ger")))) egg.add(s);
            /*if(already.contains(toSDName(o.getString("ger")))) continue;
            already.add(toSDName(o.getString("ger")));
            st.add("(" + Stream.of(toSDName(o.getString("engl")), toSDName(o.getString("ger")), o.getString("ger"), o.getString("type"), "normal").map(str -> "\"" + str + "\"").collect(Collectors.joining(",")) + ")");*/
        }
        for (String s : egg) {
            st.add("(" + Stream.of(s, toSDName(t.getJSONObject(s).getString("ger")), "", t.getJSONObject(s).getString("ger"), "egg", "default").map(str -> "\"" + str + "\"").collect(Collectors.joining(", ")) + ")");
        }
        Database.update("insert into translations(englishid, germanid, englishname, germanname, type, modification) values " + String.join(",", st));
    }

    @PrivateCommand(name = "delfromjson")
    public static void delFromJSON(JDA jda, MessageChannel tco, Message message) throws SQLException {
        ResultSet set = Database.select("select * from analysis");
        JSONObject an = getEmolgaJSON().getJSONObject("analyse");
        while (set.next()) {
            an.remove(set.getString("replay"));
        }
        saveEmolgaJSON();
    }

    @PrivateCommand(name = "addreactions")
    public static void addReactions(JDA jda, MessageChannel tco, Message message) {
        String[] msg = message.getContentDisplay().split("\\s+");
        jda.getTextChannelById(msg[1]).retrieveMessageById(msg[2]).queue(m -> {
            m.getReactions().forEach(mr -> {
                MessageReaction.ReactionEmote emote = mr.getReactionEmote();
                if(emote.isEmoji()) m.addReaction(emote.getEmoji()).queue();
                else m.addReaction(emote.getEmote()).queue();
            });
            tco.sendMessage("Done!").queue();
        });
    }

    public static void execute(JDA jda, MessageChannel tco, Message message) {
        String msg = message.getContentRaw();
        for (Method method : PrivateCommands.class.getDeclaredMethods()) {
            PrivateCommand a = method.getAnnotation(PrivateCommand.class);
            if (a == null) continue;
            if (msg.toLowerCase().startsWith("!" + a.name().toLowerCase() + " ") || msg.equalsIgnoreCase("!" + a.name()) || Arrays.stream(a.aliases()).anyMatch(s -> msg.startsWith("!" + s + " ") || msg.equalsIgnoreCase("!" + s))) {
                try {
                    method.invoke(null, jda, tco, message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
