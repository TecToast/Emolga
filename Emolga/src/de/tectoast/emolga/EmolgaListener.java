package de.tectoast.emolga;

import de.tectoast.commands.CommandCategory;
import de.tectoast.commands.PrivateCommands;
import de.tectoast.utils.*;
import de.tectoast.utils.Draft.Draft;
import de.tectoast.utils.Music.GuildMusicManager;
import de.tectoast.utils.Showdown.Analysis;
import de.tectoast.utils.Showdown.Player;
import de.tectoast.utils.Showdown.SDPokemon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.commands.Command.*;

public class EmolgaListener extends ListenerAdapter {
    public static final File emolgadata = new File("./emolgadata.json");
    public static final File wikidata = new File("./wikidata.json");
    public static final List<String> allowsCaps = Arrays.asList("712612442622001162", "752230819644440708", "732545253344804914");
    public static boolean disablesort = false;
    public static File file = new File("./debug.txt");
    public static boolean leon = false;
    public static boolean pizza = false;
    //public static byte[] bytes;

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent e) {
        String id = e.getChannelJoined().getId();
        Member mem = e.getMember();
        if (id.equals("712035339321081989") && mem.getId().equals("574949229668335636") && leon)
            e.getGuild().moveVoiceMember(mem, e.getGuild().getVoiceChannelById("722844712939159552")).queue();
        if (id.equals("712035339321081989") && pizza && !mem.getEffectiveName().toLowerCase().contains("pizza"))
            e.getGuild().moveVoiceMember(mem, e.getGuild().getVoiceChannelById("745957605146624024")).queue();
    }

    @Override
    public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent e) {
        String id = e.getChannelJoined().getId();
        Member mem = e.getMember();
        if (id.equals("712035339321081989") && mem.getId().equals("574949229668335636") && leon)
            e.getGuild().moveVoiceMember(mem, e.getGuild().getVoiceChannelById("722844712939159552")).queue();
        if (id.equals("712035339321081989") && pizza && !mem.getEffectiveName().toLowerCase().contains("pizza"))
            e.getGuild().moveVoiceMember(mem, e.getGuild().getVoiceChannelById("745957605146624024")).queue();
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent e) {
        Guild g = e.getGuild();
        Member mem = e.getMember();
        if (g.getId().equals("712035338846994502")) {
            g.addRoleToMember(mem, g.getRoleById("715242519528603708")).queue();
        }
        if (g.getId().equals("706794294127755324") && (mem.getId().equals("574989869911113738") || mem.getId().equals("728202578353193010"))) {
            e.getMember().ban(0, "Permanent").queue();
        }
    }

    @Override
    public void onRoleCreate(@Nonnull RoleCreateEvent e) {
        if (!e.getGuild().getId().equals("736555250118295622") && !e.getGuild().getId().equals("447357526997073930"))
            return;
        e.getRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE).queue();
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent e) {
        if (e.getAuthor().getId().equals(Constants.FLOID)) {
            PrivateCommands.fromPrivate(e);
        } /*else if (e.getAuthor().getId().equals("574949229668335636")) {
            e.getJDA().getTextChannelById("743471003220443226").sendMessage(e.getMessage().getContentDisplay()).queue();
        }*/
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent e) {
        if (e.getMember().getId().equals("723829878755164202")) {
            GuildMusicManager manager = getGuildAudioPlayer(e.getGuild());
            manager.scheduler.queue.clear();
            manager.scheduler.nextTrack();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        Draft.init();
        if (emolgajson.has("giveaways")) {
            JSONArray arr = emolgajson.getJSONArray("giveaways");
            for (Object o : arr) {
                JSONObject obj = (JSONObject) o;
                new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"), obj.has("role"));
            }
        }
        if (emolgajson.has("mutes")) {
            JSONArray arr = emolgajson.getJSONArray("mutes");
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("delay")) continue;
                long origdelay = obj.getLong("delay");
                long delay = origdelay - System.currentTimeMillis();
                if (delay < 0) {
                    indexes.add(i);
                    Guild g = e.getJDA().getGuildById(obj.getString("guild"));
                    if (g.getId().equals("712035338846994502"))
                        g.removeRoleFromMember(obj.getString("user"), g.getRoleById("717297533294215258")).queue();
                    else if (g.getId().equals("447357526997073930"))
                        g.removeRoleFromMember(obj.getString("user"), g.getRoleById("761723664273899580")).queue();
                    continue;
                }
                muteTimer(e.getJDA().getGuildById(obj.getString("guild")), origdelay, delay, obj.getString("user"));
            }
            while (!indexes.isEmpty()) {
                System.out.println(indexes);
                arr.remove(indexes.remove(0));
                for (int j = 0; j < indexes.size(); j++) {
                    indexes.set(j, indexes.get(j) - 1);
                }
            }
            saveEmolgaJSON();
        }
        if (emolgajson.has("bans")) {
            JSONArray arr = emolgajson.getJSONArray("bans");
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("delay")) continue;
                long origdelay = obj.getLong("delay");
                long delay = origdelay - System.currentTimeMillis();
                if (delay < 0) {
                    indexes.add(i);
                    e.getJDA().getGuildById(obj.getString("guild")).unban(obj.getString("user")).queue();
                    continue;
                }
                banTimer(e.getJDA().getGuildById(obj.getString("guild")), origdelay, (int) delay, obj.getString("user"));
            }
            while (!indexes.isEmpty()) {
                System.out.println(indexes);
                arr.remove(indexes.remove(0));
                for (int j = 0; j < indexes.size(); j++) {
                    indexes.set(j, indexes.get(j) - 1);
                }
            }
            saveEmolgaJSON();
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (expEdited) {
                    saveLevelJSON();
                }
            }
        }, 0, 30000);
        //e.getJDA().getTextChannelById("756828765065183253").retrieveMessageById("756828900083892264").queue(helps::add);
        e.getJDA().getTextChannelById("757170844240707634").retrieveMessageById("757171205055840286").queue(helps::add);
        //e.getJDA().getTextChannelById("715249205186265178").retrieveMessageById("758397956637720596").queue(helps::add);
        Guild g = e.getJDA().getGuildById("712035338846994502");
        TextChannel channel = e.getJDA().getTextChannelById("715249205186265178");
        //channel.retrieveMessageById("758397956637720596").queue(helps::add);
        channel.addReactionById("759407279094628383", g.getEmoteById("715932914554110065")).queue();
        channel.addReactionById("759407279094628383", g.getEmoteById("715932816910712923")).queue();
        channel.addReactionById("759407279094628383", g.getEmoteById("750666078828363888")).queue();
        JSONObject json = getEmolgaJSON();
        if (!json.has("style")) return;
        JSONArray style = json.getJSONArray("style");
        for (int i = 0; i < style.length(); i++) {
            JSONObject obj = style.getJSONObject(i);
            long delay = Long.parseLong(obj.getString("timer")) - System.currentTimeMillis();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Guild guild = e.getJDA().getGuildById("709877545708945438");
                    Message message = guild.getTextChannelById("749194448507764766").retrieveMessageById(obj.getString("mid")).complete();
                    String uid1 = obj.getString("uid1");
                    String uid2 = obj.getString("uid2");
                    String sid = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup").getJSONObject("doc").getString("sid");
                    woolooStyle(sid, message, uid1, uid2);
                }
            }, delay < 0 ? 0 : delay);
            System.out.println("TIMER: " + (delay));
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent e) {
        System.out.println(e.getOldName() + " -> " + e.getNewName());
        if (e.getUser().getMutualGuilds().stream().map(ISnowflake::getId).collect(Collectors.toList()).contains("518008523653775366"))
            e.getJDA().getTextChannelById("728675253924003870").sendMessage(e.getOldName() + " hat sich auf ganz Discord in " + e.getNewName() + " umbenannt!").queue();
    }


    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        if (e.getUser().isBot()) return;
        Member mem = e.getMember();
        Guild g = e.getGuild();
        if (e.getMessageId().equals("778380596413464676") && e.getReactionEmote().isEmote()) {
            String eid = e.getReactionEmote().getEmote().getId();
            JSONObject counter = shinycountjson.getJSONObject("counter");
            Optional<String> mop = counter.keySet().stream().filter(s -> counter.getJSONObject(s).getString("emote").equals(eid)).findFirst();
            if (mop.isPresent()) {
                String method = mop.get();
                counter.getJSONObject(method).put(mem.getId(), counter.getJSONObject(method).optInt(mem.getId(), 0) + 1);
                e.getJDA().getTextChannelById("778380440078647296").removeReactionById("778380596413464676", e.getReactionEmote().getEmote(), e.getUser()).queue();
                updateShinyCounts();
            }
        }
        if (e.getMessageId().equals("755331617970454558")) {
            e.getReaction().clearReactions().queue();
        }
        Optional<Message> op = helps.stream().filter(m -> m.getId().equals(e.getMessageId())).findFirst();
        if (op.isPresent() && e.getReaction().getReactionEmote().isEmoji()) {
            e.getReaction().removeReaction(e.getUser()).queue();
            Message m = op.get();
            String emoji = e.getReactionEmote().getEmoji();
            if (emoji.equals("◀️")) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("commands").setColor(java.awt.Color.CYAN);
                builder.setDescription(getHelpDescripion(g, mem));
                builder.setColor(java.awt.Color.CYAN);
                addReactions(m, mem);
                m.editMessage(builder.build()).queue();
            }
            CommandCategory.getOrder().stream().filter(cat -> cat.getEmoji().equals(emoji) && cat.allowsMember(mem) && cat.allowsGuild(g)).findFirst().ifPresent(c -> m.editMessage(
                    new EmbedBuilder().setTitle(c.getName()).setColor(java.awt.Color.CYAN).setDescription(getWithCategory(c, g, mem).stream().map(cmd -> cmd.getHelp(g)).collect(Collectors.joining("\n")) + "\n\u25c0\ufe0f Zurück zur Übersicht").build()).queue());

        }
    }

    @Override
    public void onTextChannelCreate(@NotNull TextChannelCreateEvent e) {
        Guild g = e.getGuild();
        if (g.getId().equals("712035338846994502"))
            e.getChannel().getManager().putPermissionOverride(g.getRoleById("717297533294215258"), null, Collections.singletonList(Permission.MESSAGE_WRITE)).queue();
        else if (g.getId().equals("447357526997073930"))
            e.getChannel().getManager().putPermissionOverride(g.getRoleById("761723664273899580"), null, Collections.singletonList(Permission.MESSAGE_WRITE)).queue();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        new Thread(() -> {
            Message m = e.getMessage();
            String msg = m.getContentDisplay();
            TextChannel tco = e.getChannel();
            Member member = e.getMember();
            Guild g = e.getGuild();
            String gid = g.getId();
            String meid = member.getId();
            if (member.getId().equals(Constants.FLOID)) {
                if (msg.contains("518008523653775366")) gid = "518008523653775366";
                if (msg.contains("709877545708945438")) gid = "709877545708945438";
                if (msg.contains("747357029714231299")) gid = "747357029714231299";
            }
            if (gid.equals("712035338846994502")) {
                if (g.getSelfMember().canInteract(member) && !allowsCaps.contains(tco.getId())) {
                    int x = 0;
                    for (char c : msg.toCharArray()) {
                        if (c >= 65 && c <= 90) x++;
                    }
                    if (msg.length() > 3 && (double) x / (double) msg.length() > 0.6) {
                        m.delete().queue();
                        warn(tco, g.getSelfMember(), member, "Capslock");
                    }
                }
                if (System.currentTimeMillis() - latestExp.getOrDefault(meid, (long) 0) > 60000 && !member.getUser().isBot()) {
                    latestExp.put(meid, System.currentTimeMillis());
                    JSONObject levelsystem = getLevelJSON();
                    int exp = levelsystem.optInt(meid, 0);
                    int oldlevel = getLevelFromXP(exp);
                    exp += (new Random().nextInt(10) + 15) * expmultiplicator.getOrDefault(meid, (double) 1);
                    int newlevel = getLevelFromXP(exp);
                    levelsystem.put(meid, exp);
                    if (newlevel != oldlevel) {
                        e.getJDA().getTextChannelById("447357526997073932").sendMessage("HGW " + member.getEffectiveName() + "! Du bist nun Level " + newlevel + "!").queue();
                        //g.getTextChannelById("732545253344804914").sendMessage(member.getAsMention() + ", du erreichst Level " + data.getInt("levels") + "!" +
                        //      (meid.equals("456821278653808650") ? "\nHaha du wurdest gepingt :^)" : "")).queue();
                    }
                    expEdited = true;
                }
            }
            if (tco.getId().equals("771350676876951552") || tco.getId().equals("774661698074050581")) {
                List<String> list = Arrays.asList(msg.split("\n"));
                JSONObject json = getEmolgaJSON().getJSONObject("BlitzTurnier");
                int gameday = json.getInt("gameday");
                if(gameday == -1) return;
                String p1 = member.getId();
                Optional<String> op = Arrays.stream(json.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";")).filter(str -> str.contains(p1)).findFirst();
                if (!op.isPresent()) {
                    sendToUser(member, "Du spielst nicht in dieser Liga mit!");
                    return;
                }
                String[] bosplit = op.get().split(":");
                String p2 = bosplit[0].equals(p1) ? bosplit[1] : bosplit[0];
                int k1 = 0;
                int d1 = 0;
                int p1wins = 0;
                int gamecount = 0;
                for (int i = 1; i < list.size(); i++) {
                    k1 += 4 - Integer.parseInt(list.get(i).split(":")[1].trim());
                    int d = Integer.parseInt(list.get(i).split(":")[0].trim());
                    d1 += 4 - d;
                    if (d > 0) p1wins++;
                    gamecount++;
                }
                boolean p1win = p1wins > (gamecount - p1wins);
                System.out.println("p1win = " + p1win);
                System.out.println("p1wins = " + p1wins);
                System.out.println("gamecount = " + gamecount);
                System.out.println("k1 = " + k1);
                System.out.println("d1 = " + d1);
                JSONObject playerstats = json.getJSONObject("playerstats");
                JSONObject obj1 = playerstats.has(p1) ? playerstats.getJSONObject(p1) : new JSONObject();
                JSONObject obj2 = playerstats.has(p2) ? playerstats.getJSONObject(p2) : new JSONObject();
                obj1.put("wins", obj1.optInt("wins", 0) + (p1win ? 1 : 0));
                obj2.put("wins", obj2.optInt("wins", 0) + (p1win ? 0 : 1));
                obj1.put("looses", obj1.optInt("looses", 0) + (p1win ? 0 : 1));
                obj2.put("looses", obj2.optInt("looses", 0) + (p1win ? 1 : 0));

                obj1.put("bo3wins", obj1.optInt("bo3wins", 0) + p1wins);
                obj2.put("bo3wins", obj2.optInt("bo3wins", 0) + (gamecount - p1wins));
                obj1.put("bo3looses", obj1.optInt("bo3looses", 0) + (gamecount - p1wins));
                obj2.put("bo3looses", obj2.optInt("bo3looses", 0) + p1wins);

                obj1.put("kills", obj1.optInt("kills", 0) + k1);
                obj2.put("kills", obj2.optInt("kills", 0) + d1);
                obj1.put("deaths", obj1.optInt("deaths", 0) + d1);
                obj2.put("deaths", obj2.optInt("deaths", 0) + k1);

                playerstats.put(p1, obj1);
                playerstats.put(p2, obj2);

                JSONObject results = json.getJSONObject("results");
                JSONObject rg = results.has(String.valueOf(gameday)) ? results.getJSONObject(String.valueOf(gameday)) : new JSONObject();
                rg.put(p1 + ":" + p2, p1win ? p1 : p2);
                results.put(String.valueOf(gameday), rg);
                updateTable(json, e.getJDA().getTextChannelById("771403849029386270"));
                if (rg.length() == 4) {
                    if (json.getInt("gameday") == 4) {
                        sendToMe("Mach die drecks POs du knecht!");
                        json.put("gameday", -1);
                        saveEmolgaJSON();
                        return;
                    }
                    new Thread(() -> {
                        JSONObject bo = json.getJSONObject("battleorder");
                        ArrayList<String> already = bo.keySet().stream().map(bo::getString).flatMap(s -> Arrays.stream(s.split(";"))).collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<String> order = new ArrayList<>();
                        ArrayList<String> names = getBlitzTable(true).stream().map(l -> (String) l.get(0)).collect(Collectors.toCollection(ArrayList::new));
                        HashMap<String, String> namesmap = new HashMap<>();
                        EmolgaMain.jda.getGuildById(Constants.BSID).retrieveMembersByIds(names.toArray(new String[0])).get().forEach(mem -> namesmap.put(mem.getId(), mem.getEffectiveName()));
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
                        json.put("gameday", json.getInt("gameday") + 1);
                        StringBuilder str = new StringBuilder("Runde " + json.getInt("gameday") + ":\n");
                        for (String s : order) {
                            str.append(namesmap.get(s.split(":")[0])).append(" vs ").append(namesmap.get(s.split(":")[1])).append("\n");
                        }
                        e.getJDA().getTextChannelById("771403897130450995").sendMessage(str.toString()).queue();
                        bo.put(String.valueOf(json.getInt("gameday")), String.join(";", order));
                        saveEmolgaJSON();
                    }).start();
                }
                saveEmolgaJSON();
            }
            if (tco.getId().equals("759712094223728650") || tco.getId().equals("759734608773775360")) {
                JSONObject bst = getEmolgaJSON().getJSONObject("BST");
                String raw = m.getContentRaw();
                System.out.println("raw = " + raw);
                ArrayList<String> list = new ArrayList<>(Arrays.asList(msg.split("\n")));
                int gameday = bst.getInt("gameday");
                ArrayList<String> gdl = new ArrayList<>(Arrays.asList(bst.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";")));
                String p1;
                if (as != null && member.getId().equals(Constants.FLOID))
                    p1 = as.getId();
                else p1 = member.getId();
                Optional<String> op = gdl.stream().filter(str -> str.contains(p1)).findFirst();
                if (!op.isPresent()) {
                    sendToUser(member, "Du spielst nicht in dieser Liga mit!");
                    return;
                }
                String[] bosplit = op.get().split(":");
                String p2 = bosplit[0].equals(p1) ? bosplit[1] : bosplit[0];
                System.out.println("p1 = " + p1);
                System.out.println("p2 = " + p2);
                String sid = bst.getString("sid");
                if (bst.has("results")) {
                    JSONObject res = bst.getJSONObject("results");
                    if (res.has(String.valueOf(gameday))) {
                        JSONObject obj = res.getJSONObject(String.valueOf(gameday));
                        if (obj.has(p1 + ";" + p2) || obj.has(p2 + ";" + p1)) {
                            m.delete().queue();
                            member.getUser().openPrivateChannel().queue(pc -> pc.sendMessage("Dieser Kampf ist bereits eingetragen!").queue());
                            return;
                        }
                    }
                }
                ArrayList<String> errmsg = new ArrayList<>(list);
                boolean err = false;
                ArrayList<String> mons1 = new ArrayList<>();
                ArrayList<String> mons2 = new ArrayList<>();
                int x = 11;
                for (String s : list.subList(11, 17)) {
                    String gername = getBSTGerName(s.split(" ")[0]);
                    if (!gername.startsWith("pkmn;") || gername.equals("ONLYWITHFORM")) {
                        err = true;
                        errmsg.set(x, errmsg.get(x) + " **<--**");
                        x++;
                        continue;
                    }
                    mons1.add(gername.substring(5));
                    x++;
                }
                x = 20;
                for (String s : list.subList(20, 26)) {
                    String gername = getBSTGerName(s.split(" ")[0]);
                    if (!gername.startsWith("pkmn;") || gername.equals("ONLYWITHFORM")) {
                        err = true;
                        errmsg.set(x, errmsg.get(x) + " **<--**");
                        x++;
                        continue;
                    }
                    mons2.add(gername.substring(5));
                    x++;
                }
                if (err) {
                    m.delete().queue();
                    member.getUser().openPrivateChannel().queue(pc -> pc.sendMessage("Es ist ein Fehler aufgetreten!\n\n" + String.join("\n", errmsg)).queue());
                    return;
                }
                ArrayList<Object> p1m = new ArrayList<>();
                ArrayList<Object> p2m = new ArrayList<>();
                for (String s : mons1) {
                    try {
                        String sprite = getIconSprite(s);
                        if (sprite.equals("")) throw new Exception();
                        p1m.add(sprite);
                    } catch (Exception exception) {
                        err = true;
                        int index = mons1.indexOf(s) + 11;
                        errmsg.set(index, errmsg.get(index) + " **<--**");
                    }
                }
                for (String s : mons2) {
                    try {
                        String sprite = getIconSprite(s);
                        if (sprite.equals("")) throw new Exception();
                        p2m.add(sprite);
                    } catch (Exception exception) {
                        err = true;
                        int index = mons2.indexOf(s) + 20;
                        errmsg.set(index, errmsg.get(index) + " **<--**");
                    }
                }
                if (err) {
                    m.delete().queue();
                    member.getUser().openPrivateChannel().queue(pc -> pc.sendMessage("Es ist ein Fehler aufgetreten!\n\n" + String.join("\n", errmsg)).queue());
                    return;
                }
                HashMap<String, Integer> k1 = new HashMap<>();
                HashMap<String, Integer> k2 = new HashMap<>();
                HashMap<String, Integer> d1 = new HashMap<>();
                HashMap<String, Integer> d2 = new HashMap<>();
                HashMap<String, List<Integer>> uses1 = new HashMap<>();
                HashMap<String, List<Integer>> uses2 = new HashMap<>();
                ArrayList<String> results = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(i + ": " + list.get(i));
                }
                for (int i = 11; i <= (list.size() > 48 ? 55 : 33); i += 22) {
                    try {
                        results.add(list.get(i - 4).split(" ")[1]);
                    } catch (Exception exception) {
                        errmsg.set(i - 4, list.get(i - 4) + " **<--**");
                        err = true;
                    }
                    for (int j = 0; j < 15; j++) {
                        if (j >= 6 && j <= 8) continue;
                        try {
                            String[] split = list.get(i + j).split(" ");
                            if (split[1].equals("/")) continue;
                            String mon = getBSTGerName(split[0]);
                            if (!mon.startsWith("pkmn;") || mon.equals("ONLYWITHFORM")) throw new Exception();
                            mon = mon.substring(5);
                            if (j <= 5) {
                                if (!uses1.containsKey(mon)) uses1.put(mon, new ArrayList<>());
                                uses1.get(mon).add((i + 12) / 22);
                                if (!k1.containsKey(mon)) k1.put(mon, 0);
                                if (!d1.containsKey(mon)) d1.put(mon, 0);
                                k1.put(mon, k1.get(mon) + Integer.parseInt(split[1]));
                                d1.put(mon, d1.get(mon) + (split[2].equalsIgnoreCase("d") ? 1 : 0));
                            } else {
                                if (!uses2.containsKey(mon)) uses2.put(mon, new ArrayList<>());
                                uses2.get(mon).add((i + 12) / 22);
                                if (!k2.containsKey(mon)) k2.put(mon, 0);
                                if (!d2.containsKey(mon)) d2.put(mon, 0);
                                k2.put(mon, k2.get(mon) + Integer.parseInt(split[1]));
                                d2.put(mon, d2.get(mon) + (split[2].equalsIgnoreCase("d") ? 1 : 0));
                            }
                        } catch (Exception ex) {
                            errmsg.set(i + j, errmsg.get(i + j) + " **<--**");
                            err = true;
                        }
                    }
                }
                if (err) {
                    m.delete().queue();
                    member.getUser().openPrivateChannel().queue(pc -> pc.sendMessage("Es ist ein Fehler aufgetreten!\n\n" + String.join("\n", errmsg)).queue());
                    return;
                }
                System.out.println("Spieler 1:");
                for (String s : mons1) {
                    System.out.println(s + " K: " + (k1.containsKey(s) ? k1.get(s) : "0") + " D: " + (d1.containsKey(s) ? d1.get(s) : "0"));
                }
                System.out.println("Spieler 2:");
                for (String s : mons2) {
                    System.out.println(s + " K: " + (k2.containsKey(s) ? k2.get(s) : "0") + " D: " + (d2.containsKey(s) ? d2.get(s) : "0"));
                }
                List<List<Object>> r1 = new ArrayList<>();
                List<List<Object>> r2 = new ArrayList<>();
                ArrayList<Integer> wins1 = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    String[] split = results.get(i).split(":");
                    if (!split[0].equals("0")) wins1.add(i + 1);
                }
                r1.add(Collections.singletonList(wins1.size()));
                r2.add(Collections.singletonList(results.size() - wins1.size()));

                for (String result : results) {
                    String[] split = result.split(":");
                    r1.add(Collections.singletonList(split[0]));
                    r2.add(Collections.singletonList(split[1]));
                }
                JSONObject obj = getStatisticsJSON();
                ArrayList<String> statorder = new ArrayList<>();
                if (!obj.getString("order").equals(""))
                    statorder.addAll(Arrays.asList(obj.getString("order").split(",")));
                ArrayList<String> origstatorder = new ArrayList<>(statorder);
                System.out.println("origstatorder1 = " + origstatorder);
                for (String s : mons1) {
                    if (!obj.has(s)) {
                        obj.put(s, new JSONObject());
                        statorder.add(s);
                    }
                    JSONObject o = obj.getJSONObject(s);
                    if (!o.has("usagerate")) o.put("usagerate", 0);
                    if (!o.has("winrate")) o.put("winrate", 0);
                    if (!o.has("kills")) o.put("kills", 0);
                    if (!o.has("deaths")) o.put("deaths", 0);
                    o.put("usagerate", o.getInt("usagerate") + (uses1.getOrDefault(s, Collections.emptyList()).size()));
                    int wins = 0;
                    for (Integer integer : uses1.getOrDefault(s, Collections.emptyList())) {
                        if (wins1.contains(integer)) wins++;
                    }
                    o.put("winrate", o.getInt("winrate") + wins);
                    o.put("kills", o.getInt("kills") + k1.getOrDefault(s, 0));
                    o.put("deaths", o.getInt("deaths") + d1.getOrDefault(s, 0));

                }
                for (String s : mons2) {
                    if (!obj.has(s)) {
                        obj.put(s, new JSONObject());
                        statorder.add(s);
                    }
                    JSONObject o = obj.getJSONObject(s);
                    if (!o.has("usagerate")) o.put("usagerate", 0);
                    if (!o.has("winrate")) o.put("winrate", 0);
                    if (!o.has("kills")) o.put("kills", 0);
                    if (!o.has("deaths")) o.put("deaths", 0);
                    o.put("usagerate", o.getInt("usagerate") + (uses2.getOrDefault(s, Collections.emptyList()).size()));
                    int wins = 0;
                    for (Integer integer : uses2.getOrDefault(s, Collections.emptyList())) {
                        if (!wins1.contains(integer)) wins++;
                    }
                    o.put("winrate", o.getInt("winrate") + wins);
                    o.put("kills", o.getInt("kills") + k2.getOrDefault(s, 0));
                    o.put("deaths", o.getInt("deaths") + d2.getOrDefault(s, 0));
                }
                System.out.println("origstatorder2 = " + origstatorder);
                obj.put("order", String.join(",", statorder));
                obj.put("games", obj.getInt("games") + results.size());
                saveStatisticsJSON();
                //System.out.println("k1 = " + k1);
                //System.out.println("k2 = " + k2);
                ArrayList<String> tab = new ArrayList<>(Arrays.asList(bst.getString("table").split(",")));
                int index1 = tab.indexOf(p1);
                int index2 = tab.indexOf(p2);
                int x1 = index1;
                int y1 = 0;
                while (index1 - 4 * y1 > 3) {
                    x1 -= 4;
                    y1++;
                }
                int x2 = index2;
                int y2 = 0;
                while (index2 - 4 * y2 > 3) {
                    x2 -= 4;
                    y2++;
                }
                x1 = x1 * 10 + 3;
                y1 = y1 * 18 + 3;
                x2 = x2 * 10 + 3;
                y2 = y2 * 18 + 3;
                if (y1 == 75) x1 += 10;
                if (y2 == 75) x2 += 10;
                //List<Object> gwins1 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x1) + y1 + ":" + getAsXCoord(x1 + 1) + y1, false, false).get(0);
                //List<Object> gwins2 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x2) + y2 + ":" + getAsXCoord(y1 + 1) + y2, false, false).get(0);
                if (!bst.has("playerstats")) bst.put("playerstats", new JSONObject());
                JSONObject playerstats = bst.getJSONObject("playerstats");
                if (!playerstats.has(p1)) playerstats.put(p1, new JSONObject());
                if (!playerstats.has(p2)) playerstats.put(p2, new JSONObject());
                JSONObject stat1 = playerstats.getJSONObject(p1);
                JSONObject stat2 = playerstats.getJSONObject(p2);
                if (!stat1.has("wins")) stat1.put("wins", 0);
                if (!stat2.has("wins")) stat2.put("wins", 0);
                if (!stat1.has("looses")) stat1.put("looses", 0);
                if (!stat2.has("looses")) stat2.put("looses", 0);
                if (!stat1.has("kills")) stat1.put("kills", 0);
                if (!stat2.has("kills")) stat2.put("kills", 0);
                if (!stat1.has("deaths")) stat1.put("deaths", 0);
                if (!stat2.has("deaths")) stat2.put("deaths", 0);
                if (wins1.size() == 2) {
                    stat1.put("wins", stat1.getInt("wins") + 1);
                    stat2.put("looses", stat2.getInt("looses") + 1);
                } else {
                    stat2.put("wins", stat2.getInt("wins") + 1);
                    stat1.put("looses", stat1.getInt("looses") + 1);
                }
                RequestBuilder b = new RequestBuilder(sid);
                b.addRow("Teilnehmer!" + getAsXCoord(x1) + y1, Arrays.asList(stat1.getInt("wins"), stat1.getInt("looses")))
                        .addRow("Teilnehmer!" + getAsXCoord(x2) + y2, Arrays.asList(stat2.getInt("wins"), stat2.getInt("looses")));
                int k1sum = k1.values().stream().reduce(Integer::sum).orElse(0);
                int d1sum = d1.values().stream().reduce(Integer::sum).orElse(0);
                stat1.put("kills", stat1.getInt("kills") + k1sum);
                stat2.put("kills", stat2.getInt("kills") + d1sum);
                stat1.put("deaths", stat1.getInt("deaths") + d1sum);
                stat2.put("deaths", stat2.getInt("deaths") + k1sum);
                if (!bst.has("results")) bst.put("results", new JSONObject());
                JSONObject resultsj = bst.getJSONObject("results");
                if (!resultsj.has(String.valueOf(gameday))) resultsj.put(String.valueOf(gameday), new JSONObject());
                JSONObject resgd = resultsj.getJSONObject(String.valueOf(gameday));
                resgd.put(p1 + ";" + p2, wins1.size() == 2 ? p1 : p2);
                saveEmolgaJSON();
                b.addRow("Teilnehmer!" + getAsXCoord(x1 + 5) + (y1 + gameday + 3), Arrays.asList(k1sum, d1sum))
                        .addRow("Teilnehmer!" + getAsXCoord(x2 + 5) + (y2 + gameday + 3), Arrays.asList(d1sum, k1sum));
                int ip = gdl.indexOf(gdl.stream().filter(s -> s.contains(p1)).collect(Collectors.joining("")));
                int yy = gameday == 9 ? (ip * 8 + 4) : gameday == 10 ? (ip * 16 + 8) : 16;
                if (gdl.get(ip).split(":")[0].equals(p1)) {
                    b
                            .addAll("Vorrunde!" + getAsXCoord((gameday - 8) * 7 - 4) + yy, r1)
                            .addAll("Vorrunde!" + getAsXCoord((gameday - 8) * 7 - 2) + yy, r2);
                } else {
                    b
                            .addAll("Vorrunde!" + getAsXCoord((gameday - 8) * 7 - 4) + yy, r2)
                            .addAll("Vorrunde!" + getAsXCoord((gameday - 8) * 7 - 2) + yy, r1);
                }
                List<Object> get1 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + 2) + ":" + getAsXCoord(x1) + (y1 + 2), false, false).get(0);
                List<Object> get2 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + 2) + ":" + getAsXCoord(x2) + (y2 + 2), false, false).get(0);
                b.addRow("Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + 2), Arrays.asList(Integer.parseInt((String) get1.get(0)) + wins1.size(), Integer.parseInt((String) get1.get(1)) + (results.size() - wins1.size())))
                        .addRow("Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + 2), Arrays.asList(Integer.parseInt((String) get2.get(0)) + (results.size() - wins1.size()), Integer.parseInt((String) get1.get(1)) + wins1.size()))
                        .addRow("Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + gameday + 3), p1m)
                        .addRow("Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + gameday + 3), p2m);
                List<List<Object>> statsend = new ArrayList<>();
                int games = obj.getInt("games");
                int i = 3;
                for (String s : statorder) {
                    JSONObject o = obj.getJSONObject(s);
                    int urate = o.getInt("usagerate");
                    int winrate = o.getInt("winrate");
                    int kills = o.getInt("kills");
                    int deaths = o.getInt("deaths");
                    System.out.println("s = " + s);
                    statsend.add(Arrays.asList(
                            urate,
                            "=RUNDEN(C" + i + " / A1; 4)",
                            winrate,
                            "=WENN(C" + i + " = 0; 0; RUNDEN(E" + i + " / C" + i + "; 4))",
                            kills,
                            "=WENN(C" + i + " = 0; 0; RUNDEN(G" + i + " / C" + i + "; 2))",
                            deaths,
                            "=WENN(C" + i + " = 0; 0; RUNDEN(I" + i + " / C" + i + "; 2))"));
                    i++;
                }
                b.addAll("Statistiken!C3", statsend);
                b.addSingle("Statistiken!A1", games);
                System.out.println("statorder1 = " + statorder);
                System.out.println("origstatorder3 = " + statorder);
                statorder.removeAll(origstatorder);
                System.out.println("statorder2 = " + statorder);
                List<List<Object>> newmons = new ArrayList<>();
                for (String s : statorder) {
                    newmons.add(Arrays.asList(s, "=IMAGE(\"" + getSugiLink(s) + "\")"));
                }
                b.addAll("Statistiken!A" + (origstatorder.size() + 3), newmons);
                b.execute();
                System.out.println("Updating...");
                sortBST();
                return;
            }
            if (tco.getId().equals("743471003220443226") && !member.getUser().isBot()) {
                e.getJDA().retrieveUserById("574949229668335636").complete().openPrivateChannel().complete().sendMessage(msg).queue();
                return;
            }
            check(e);
            if (gid.equals("447357526997073930")) {
                PrivateCommands.fromGuild(e);
            }
            if (tco.getId().equals("758198459563114516")) {
                g.addRoleToMember(member, g.getRoleById("758254829885456404")).queue();
            }
            if (m.getMentionedMembers().size() == 1) {
                if (m.getMentionedMembers().get(0).getId().equals("723829878755164202") && !e.getAuthor().isBot()) {
                    help(tco, member);
                }
            }
            if ((tco.getId().equals("712612442622001162") || tco.getId().equals("724034089891397692")) && m.getAttachments().size() > 0) {
                tco.sendMessage("Gz!").queue();
            }
            if (emotesteal.contains(tco.getId())) {
                List<Emote> l = m.getEmotes();
                for (Emote emote : l) {
                    try {
                        g.createEmote(emote.getName(), Icon.from(new URL(emote.getImageUrl()).openStream())).queue();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
            if (tco.getId().equals("778380440078647296")) {
                String[] split = msg.split(" ");
                JSONObject counter = shinycountjson.getJSONObject("counter");
                if (m.getEmotes().size() == 0) return;
                String eid = m.getEmotes().get(0).getId();
                Optional<String> mop = counter.keySet().stream().filter(s -> counter.getJSONObject(s).getString("emote").equals(eid)).findFirst();
                if (mop.isPresent()) {
                    JSONObject o = shinycountjson.getJSONObject("counter").getJSONObject(mop.get());
                    boolean isCmd = true;
                    if (msg.startsWith("!set ")) {
                        o.put(member.getId(), Integer.parseInt(split[1]));
                    } else if (msg.startsWith("!reset ")) {
                        o.put(member.getId(), 0);
                    } else if (msg.startsWith("!add ")) {
                        o.put(member.getId(), o.optInt(member.getId(), 0) + Integer.parseInt(split[1]));
                    } else isCmd = false;
                    if (isCmd) {
                        m.delete().queue();
                        updateShinyCounts();
                    }
                }
            }
            if (meid.equals("159985870458322944") && g.getId().equals("712035338846994502")) {
                if (msg.contains(", du erreichst Level ")) {
                    try {
                        Member mem = m.getMentionedMembers().get(0);
                        String[] split = msg.split(" ");
                        int lvl = Integer.parseInt(split[split.length - 1].substring(0, split[split.length - 1].length() - 1));
                        /*
                         * Mitglied: 715242519528603708
                         * Neuling: 715248666008354847
                         * pokemon Fan: 715247650018164826
                         * Experte: 715248788314259546
                         * Veteran: 715248194732163163
                         * Top 4: 715248297471770640
                         * Champ: 715248393223667753
                         * Elite: 715247811687612547
                         * Meistertrainer: 715248587344183347
                         * */
                        switch (lvl) {
                            case 5:
                                g.addRoleToMember(mem, g.getRoleById("715248666008354847")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715242519528603708")).queue();
                                break;
                            case 10:
                                g.addRoleToMember(mem, g.getRoleById("715247650018164826")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715248666008354847")).queue();
                                break;
                            case 15:
                                g.addRoleToMember(mem, g.getRoleById("715248788314259546")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715247650018164826")).queue();
                                break;
                            case 20:
                                g.addRoleToMember(mem, g.getRoleById("715248194732163163")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715248788314259546")).queue();
                                break;
                            case 30:
                                g.addRoleToMember(mem, g.getRoleById("715248297471770640")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715248194732163163")).queue();
                                break;
                            case 35:
                                g.addRoleToMember(mem, g.getRoleById("715248393223667753")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715248297471770640")).queue();
                                break;
                            case 40:
                                g.addRoleToMember(mem, g.getRoleById("715247811687612547")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715248393223667753")).queue();
                                break;
                            case 50:
                                g.addRoleToMember(mem, g.getRoleById("715248587344183347")).queue();
                                g.removeRoleFromMember(mem, g.getRoleById("715247811687612547")).queue();
                                break;
                        }
                    } catch (Exception ex) {
                        e.getJDA().retrieveUserById(Constants.FLOID).complete().openPrivateChannel().complete().sendMessage("Fehler bei Level Up!").queue();
                    }
                }
                return;
            }
            DexQuiz quiz = DexQuiz.getByTC(tco);
            if (quiz != null) {
                String mon = quiz.gerName;
                String name = getGerName(msg);
                if (name.startsWith("pkmn;")) name = name.split(";")[1];
                if (name.equalsIgnoreCase(mon) || name.equalsIgnoreCase(quiz.englName)) {
                    tco.sendMessage(member.getAsMention() + " hat das pokemon erraten! Es war " + mon + "!").queue();
                    quiz.round++;
                    if (!quiz.points.containsKey(member)) quiz.points.put(member, 0);
                    quiz.points.put(member, quiz.points.get(member) + 1);
                    if (quiz.round > quiz.cr) {
                        StringBuilder builder = new StringBuilder("Punkte:\n");
                        for (Map.Entry<Member, Integer> en : quiz.points.entrySet()) {
                            builder.append(en.getKey().getAsMention()).append(": ").append(en.getValue()).append("\n");
                        }
                        tco.sendMessage(builder.toString()).queue();
                        DexQuiz.list.remove(quiz);
                        return;
                    }
                    File file = new File("./entwicklung.txt");
                    try {
                        List<String> list = Files.readAllLines(file.toPath());
                        String pokemon = list.get(new Random().nextInt(list.size()));
                        Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
                        String englName = getEnglName(pokemon);
                        Element table = d.select("table[class=\"round centered\"]").get(0);
                        Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));
                        quiz.gerName = pokemon;
                        sendToMe(pokemon);
                        quiz.englName = englName;
                        //ü = %C3%B6
                        Thread.sleep(3000);
                        tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem pokemon gehört dieser Dex-Eintrag?").queue();
                    } catch (IOException | InterruptedException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
            JSONObject json = getEmolgaJSON();
            JSONObject analysis = json.getJSONObject("analyse");
            if (analysis.keySet().contains(tco.getId())) {
                if (msg.contains("https://")) {
                    Optional<String> urlop = Arrays.stream(msg.split("\n")).filter(s -> s.contains("https://replay.pokemonshowdown.com")).map(s -> s.substring(s.indexOf("https://"), s.indexOf(" ", s.indexOf("https://") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("https://") + 1))).findFirst();
                    if (urlop.isPresent()) {
                        String url = urlop.get();
                        System.out.println(url);
                        Player[] game = Analysis.analyse(url);
                        System.out.println("Analysed!");
                        if (game == null) {
                            tco.getGuild().getTextChannelById(analysis.getString(tco.getId())).sendMessage("Da in einem der beiden Teams ein Zoroark ist, kann ich das Ergebnis nicht bestimmen! Trage die Ergebnisse bitte selber ein!").queue();
                            return;
                        }
                        int aliveP1 = 0;
                        int aliveP2 = 0;
                        StringBuilder t1 = new StringBuilder();
                        StringBuilder t2 = new StringBuilder();
                        for (SDPokemon p : game[0].getMons()) {
                            if (!p.isDead()) aliveP1++;
                        }
                        for (SDPokemon p : game[1].getMons()) {
                            if (!p.isDead()) aliveP2++;
                        }
                        String winloose = aliveP1 + ":" + aliveP2;
                        boolean p1wins = game[0].isWinner();
                        HashMap<String, String> kills = new HashMap<>();
                        HashMap<String, String> deaths = new HashMap<>();
                        boolean spoiler = false;
                        if (json.has("spoiler")) {
                            spoiler = json.getJSONArray("spoiler").toList().contains(tco.getGuild().getId());
                        }
                        if (spoiler) t1.append("||");
                        for (SDPokemon p : game[0].getMons()) {
                            String monName = getMonName(p.getPokemon(), gid);
                            kills.put(monName, String.valueOf(p.getKills()));
                            deaths.put(monName, p.isDead() ? "1" : "0");
                            t1.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (p1wins || spoiler) ? "X" : "").append("\n");
                        }
                        if (spoiler) t1.append("||");
                        if (spoiler) t2.append("||");
                        for (SDPokemon p : game[1].getMons()) {
                            String monName = getMonName(p.getPokemon(), gid);
                            kills.put(monName, String.valueOf(p.getKills()));
                            deaths.put(monName, p.isDead() ? "1" : "0");
                            t2.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (!p1wins || spoiler) ? "X" : "").append("\n");
                        }
                        if (spoiler) t2.append("||");
                        System.out.println("Kills");
                        System.out.println(kills);
                        System.out.println("Deaths");
                        System.out.println(deaths);
                        String u1 = game[0].getNickname();
                        String u2 = game[1].getNickname();
                        String name1;
                        String name2;
                        String uid1 = null;
                        String uid2 = null;
                        if (json.getJSONObject("showdown").has(gid)) {
                            JSONObject showdown = json.getJSONObject("showdown").getJSONObject(gid);
                            System.out.println("u1 = " + u1);
                            System.out.println("u2 = " + u2);
                            for (String s : showdown.keySet()) {
                                if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                                if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
                            }
                            name1 = uid1 != null && gid.equals("518008523653775366") ? uid1.equals("LSD") ? "REPLACELSD" : e.getJDA().getGuildById(gid).retrieveMemberById(uid1).complete().getEffectiveName() : game[0].getNickname();
                            name2 = uid2 != null && gid.equals("518008523653775366") ? uid2.equals("LSD") ? "REPLACELSD" : e.getJDA().getGuildById(gid).retrieveMemberById(uid2).complete().getEffectiveName() : game[1].getNickname();
                        } else {
                            name1 = game[0].getNickname();
                            name2 = game[1].getNickname();
                        }
                        String str;
                        if (spoiler) {
                            str = name1 + " ||" + winloose + "|| " + name2 + "\n\n" + name1 + ":\n" + t1.toString()
                                    + "\n" + name2 + ": " + "\n" + t2.toString();
                        } else {
                            str = name1 + " " + winloose + " " + name2 + "\n\n" + name1 + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1.toString()
                                    + "\n" + name2 + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2.toString();
                        }
                        if (!gid.equals("518008523653775366")) {
                            tco.getGuild().getTextChannelById(analysis.getString(tco.getId())).sendMessage(str).queue();
                            System.out.println("In emolga Listener!");
                        }
                        if (!gid.equals("518008523653775366") && !gid.equals("447357526997073930") && !gid.equals("709877545708945438") && !gid.equals("747357029714231299"))
                            return;
                        if (!json.getJSONObject("showdown").has(gid)) return;
                        if (uid1 == null || uid2 == null) return;
                        if (sdAnalyser.containsKey(gid)) {
                            sdAnalyser.get(gid).analyse(game, uid1, uid2, kills, deaths, str, e.getGuild().getId().equals("447357526997073930") ? "447357526997073932" : "766733920309477398");
                        }
                    }
                }
            }

        }).start();
    }


    public boolean isAdmin(Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR) || member.getId().equals("598199247124299776");
    }


}
