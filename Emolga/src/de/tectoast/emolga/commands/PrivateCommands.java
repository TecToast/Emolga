package de.tectoast.emolga.commands;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.PrivateCommand;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class PrivateCommands {

    @PrivateCommand(name = "updatetierlist")
    public static void updateTierlist(GenericCommandEvent e) {
        Tierlist.setup();
        e.reply("Die Tierliste wurde aktualisiert!");
    }

    @PrivateCommand(name = "skip")
    public static void skip(GenericCommandEvent e) {
        Draft.drafts.stream().filter(draft -> draft.name.equals(e.getMsg().substring(6))).collect(Collectors.toList()).get(0).timer();
    }

    @PrivateCommand(name = "timer")
    public static void timer(GenericCommandEvent e) {
        String name = e.getMsg().substring(7);
        List<Draft> list = Draft.drafts.stream().filter(d -> d.name.equals(name)).collect(Collectors.toList());
        if (list.size() == 0) {
            e.reply("Dieser Draft existiert nicht!");
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
        e.reply("Der Timer bei " + name + " wurde aktiviert!");
    }

    @PrivateCommand(name = "edit")
    public static void edit(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).editMessageById(e.getArg(1), e.getMsg().substring(43)).queue();
    }

    @PrivateCommand(name = "send")
    public static void send(GenericCommandEvent e) {
        Message message = e.getMessage();
        String[] split = message.getContentDisplay().split(" ");
        System.out.println(message.getContentRaw());
        String s = message.getContentRaw().substring(24).replaceAll("\\\\", "");
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Guild g = tc.getGuild();
        for (ListedEmote emote : g.retrieveEmotes().complete()) {
            s = s.replace("<<" + emote.getName() + ">>", emote.getAsMention());
        }
        tc.sendMessage(s).queue();
    }

    @PrivateCommand(name = "react")
    public static void react(GenericCommandEvent e) {
        String msg = e.getMsg();
        String s = msg.substring(45);
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Message m = tc.retrieveMessageById(e.getArg(1)).complete();
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
    public static void ban(GenericCommandEvent e) {
        e.getJDA().getGuildById(e.getArg(0)).ban(e.getArg(1), 0).queue();
    }

    @PrivateCommand(name = "updatedatabase")
    public static void updateDatabase(GenericCommandEvent e) {
        loadJSONFiles();
        e.done();
    }

    @PrivateCommand(name = "emolgajson", aliases = {"ej"})
    public static void emolgajson(GenericCommandEvent e) {
        emolgajson = load("./emolgadata.json");
        e.done();
    }

    @PrivateCommand(name = "updategiveaways")
    public static void updateGiveaways(GenericCommandEvent e) {
        Giveaway.giveaways.clear();
        if (emolgajson.has("giveaways")) {
            JSONArray arr = emolgajson.getJSONArray("giveaways");
            for (Object o : arr) {
                JSONObject obj = (JSONObject) o;
                new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"), obj.has("role"));
            }
        }
        e.done();
    }

    @PrivateCommand(name = "sortbst")
    public static void sortBSTCmd(GenericCommandEvent e) {
        sortBST();
        e.done();
    }

    @PrivateCommand(name = "del")
    public static void del(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).deleteMessageById(e.getArg(1)).queue();
    }

    @PrivateCommand(name = "sortzbs")
    public static void sortZBSCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + e.getArg(0));
        sortZBS(league.getString("sid"), "Liga " + e.getArg(0), league);
    }

    @PrivateCommand(name = "sortwooloo")
    public static void sortWoolooCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup");
        sortWooloo(league.getJSONObject("doc").getString("sid"), league);
    }

    @PrivateCommand(name = "troll")
    public static void troll(GenericCommandEvent e) {
        Category category = e.getJDA().getCategoryById(e.getArg(0));
        Guild g = category.getGuild();
        Member user = g.retrieveMemberById(e.getArg(1)).complete();
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
    public static void updateTableCmd(GenericCommandEvent e) {
        updateTable(getEmolgaJSON().getJSONObject("BlitzTurnier"), e.getJDA().getTextChannelById(771403849029386270L));
    }

    @PrivateCommand(name = "nextround")
    public static void nextRoundCmd(GenericCommandEvent e) {
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
            e.reply(str.toString());
            b.put("gameday", b.getInt("gameday") + 1);
            bo.put(String.valueOf(b.getInt("gameday")), String.join(";", order));
            saveEmolgaJSON();
        }).start();
    }

    @PrivateCommand(name = "levels")
    public static void levels(GenericCommandEvent e) {
        new Thread(() -> {
            HashMap<String, String> map = new HashMap<>();
            JSONObject level = getLevelJSON();
            e.getJDA().getGuildById(Constants.BSID).retrieveMembersByIds(level.keySet().toArray(new String[0])).get().forEach(m -> map.put(m.getId(), m.getEffectiveName()));
            StringBuilder str = new StringBuilder();
            level.keySet().stream().sorted(Comparator.comparing(level::getInt).reversed()).forEach(s -> {
                if (map.get(s) == null) return;
                str.append(map.get(s)).append(": ").append(getLevelFromXP(level.getInt(s))).append("\n");
            });
            e.reply(str.toString());
        }).start();
    }

    @PrivateCommand(name = "generateorder")
    public static void generateOrder(GenericCommandEvent e) {
        new Thread(() -> {
            ArrayList<Member> list = new ArrayList<>(e.getJDA().getGuildById(Constants.BSID).findMembers(mem -> mem.getRoles().contains(e.getJDA().getGuildById(Constants.BSID).getRoleById("774659853812760587"))).get());
            Collections.shuffle(list);
            StringBuilder str = new StringBuilder();
            boolean b = false;
            for (Member member : list) {
                str.append(member.getId()).append(b ? ";" : ":");
                b = !b;
            }
            e.reply(str.toString());
        }).start();
    }

    @PrivateCommand(name = "deletedreplays")
    public static void deletedReplays(GenericCommandEvent e) {
        JSONObject a = getEmolgaJSON().getJSONObject("analyse");
        a.keySet().stream().filter(s -> e.getJDA().getTextChannelById(s) == null).collect(Collectors.toList()).forEach(a::remove);
        saveEmolgaJSON();
        updatePresence();
    }


    @PrivateCommand(name = "delfromjson")
    public static void delFromJSON(GenericCommandEvent e) throws SQLException {
        ResultSet set = Database.select("select * from analysis");
        JSONObject an = getEmolgaJSON().getJSONObject("analyse");
        while (set.next()) {
            an.remove(set.getString("replay"));
        }
        saveEmolgaJSON();
    }

    @PrivateCommand(name = "addreactions")
    public static void addReactions(GenericCommandEvent e) {
        Message message = e.getMessage();
        e.getJDA().getTextChannelById(e.getArg(0)).retrieveMessageById(e.getArg(1)).queue(m -> {
            m.getReactions().forEach(mr -> {
                MessageReaction.ReactionEmote emote = mr.getReactionEmote();
                if (emote.isEmoji()) m.addReaction(emote.getEmoji()).queue();
                else m.addReaction(emote.getEmote()).queue();
            });
            e.done();
        });
    }

    @PrivateCommand(name = "replaceplayer")
    public static void replacePlayer(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArg(0));
        JSONObject battleOrder = league.getJSONObject("battleorder");
        String oldid = e.getArg(1);
        String newid = e.getArg(2);
        for (int i = 1; i <= battleOrder.length(); i++) {
            battleOrder.put(String.valueOf(i), battleOrder.getString(String.valueOf(i)).replace(oldid, newid));
        }
        league.put("table", league.getString("table").replace(oldid, newid));
        JSONObject order = league.getJSONObject("order");
        for (int i = 1; i <= order.length(); i++) {
            order.put(String.valueOf(i), order.getString(String.valueOf(i)).replace(oldid, newid));
        }
        JSONObject picks = league.getJSONObject("picks");
        picks.put(newid, picks.getJSONArray(oldid));
        picks.remove(oldid);
        if (league.has("results")) {
            JSONObject results = league.getJSONObject("results");
            ArrayList<String> keys = new ArrayList<>(results.keySet());
            for (String key : keys) {
                if (key.contains(oldid)) {
                    results.put(key.replace(oldid, newid), results.getString(key));
                    results.remove(key);
                }
            }
        }
        saveEmolgaJSON();
        e.done();
    }

    public static void execute(Message message) {
        String msg = message.getContentRaw();
        for (Method method : PrivateCommands.class.getDeclaredMethods()) {
            PrivateCommand a = method.getAnnotation(PrivateCommand.class);
            if (a == null) continue;
            if (msg.toLowerCase().startsWith("!" + a.name().toLowerCase() + " ") || msg.equalsIgnoreCase("!" + a.name()) || Arrays.stream(a.aliases()).anyMatch(s -> msg.startsWith("!" + s + " ") || msg.equalsIgnoreCase("!" + s))) {
                try {
                    method.invoke(null, new PrivateCommandEvent(message));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
