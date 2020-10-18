package de.Flori.Emolga;

import com.google.api.services.sheets.v4.model.*;
import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.Commands.Draft.PickCommand;
import de.Flori.Commands.Giveaway.Giveaway;
import de.Flori.utils.Music.GuildMusicManager;
import de.Flori.utils.DexQuiz;
import de.Flori.utils.Draft.Draft;
import de.Flori.utils.Google;
import de.Flori.utils.Showdown.Analysis;
import de.Flori.utils.Showdown.Player;
import de.Flori.utils.Showdown.SDPokemon;
import de.Flori.utils.Draft.Tierlist;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioSendHandler;
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
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static de.Flori.Commands.Command.*;

public class EmolgaListener extends ListenerAdapter {
    public static final File emolgadata = new File("./emolgadata.json");
    public static final File wikidata = new File("./wikidata.json");


    public static boolean disablesort = false;

    public static File file = new File("./debug.txt");

    public static boolean leon = false;
    public static boolean pizza = false;

    //public static byte[] bytes;
    public static Member as = null;
    public static boolean checkBST = false;

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
        if (e.getAuthor().getId().equals("175910318608744448")) {
            String msg = e.getMessage().getContentDisplay();
            JSONObject json = getEmolgaJSON();
            String[] split = msg.split(" ");
            if (msg.startsWith("!timer")) {
                String name = msg.substring(7);
                List<Draft> list = Draft.drafts.stream().filter(d -> d.name.equals(name)).collect(Collectors.toList());
                if (list.size() == 0) {
                    sendToMe("Dieser Draft existiert nicht!");
                    return;
                }
                Draft d = list.get(0);
                d.cooldown = new Timer();
                long delay = calculateASLTimer();
                JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
                league.put("cooldown", System.currentTimeMillis() + delay);
                d.cooldown.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        d.timer();
                    }
                }, delay);
                json.getJSONObject("drafts").getJSONObject(name).put("timer", true);
                saveEmolgaJSON();
                sendToMe("Der Timer bei " + name + " wurde aktiviert!");
            } else if (msg.startsWith("!updatetierlist")) {
                Tierlist.setup();
                sendToMe("Die Tierliste wurde aktualisiert!");
            } else if (msg.startsWith("!skip")) {
                Draft.drafts.stream().filter(draft -> draft.name.equals(msg.substring(6))).collect(Collectors.toList()).get(0).timer();
            } else if (msg.startsWith("!send")) {
                System.out.println(e.getMessage().getContentRaw());
                String s = e.getMessage().getContentRaw().substring(24).replaceAll("\\\\", "");
                TextChannel tc = e.getJDA().getTextChannelById(split[1]);
                Guild g = tc.getGuild();
                for (ListedEmote emote : g.retrieveEmotes().complete()) {
                    s = s.replaceAll("<<" + emote.getName() + ">>", emote.getAsMention());
                }
                tc.sendMessage(s).queue();
            } else if (msg.startsWith("!doit")) {
                System.out.println(e.getJDA().getGuildById("709877545708945438").getRoles().stream().map(Role::getName).collect(Collectors.joining("\n")));
            } else if (msg.startsWith("!react")) {
                String s = msg.substring(45);
                TextChannel tc = e.getJDA().getTextChannelById(split[1]);
                Message m = tc.retrieveMessageById(split[2]).complete();
                assert (m != null);
                if (s.contains("<")) {
                    s = s.substring(1);
                    System.out.println("s = " + s);
                    for (ListedEmote emote : tc.getGuild().retrieveEmotes().complete()) {
                        System.out.println("emote.getName() = " + emote.getName());
                        if (s.equalsIgnoreCase(emote.getName())) {
                            m.addReaction(emote).queue();
                            break;
                        }
                    }
                } else {
                    m.addReaction(s).queue();
                }
                //System.out.println(m.getContentDisplay());
            } else if (msg.startsWith("!join")) {
                Guild g = e.getJDA().getGuildById(split[1]);
                g.getAudioManager().openAudioConnection(g.getVoiceChannelById(split[2]));
                g.getAudioManager().setSendingHandler(new AudioSendHandler() {
                    @Override
                    public boolean canProvide() {
                        return true;
                    }

                    @Nullable
                    @Override
                    public ByteBuffer provide20MsAudio() {
                        return ByteBuffer.allocate(3840);
                    }
                });
            } else if (msg.equalsIgnoreCase("!test")) {
                for (int y = 7; y <= 24; y += 17) {
                    for (int x = 68; x <= 86; x += 6) {
                        List<List<Object>> list = new ArrayList<>();
                        for (int i = 0; i < 12; i++) {
                            list.add(Arrays.asList(0, 0));
                        }
                        String str = "Teamübersicht!" + ((char) x) + y;
                        System.out.println(str);
                        System.out.println(list);
                        System.out.println();
                    }
                }
            } else if (msg.equals("!leon")) {
                leon = !leon;
                sendToMe(leon + "");
            } else if (msg.equalsIgnoreCase("!pizza")) {
                pizza = !pizza;
                sendToMe(pizza + "");
            } else if (msg.equalsIgnoreCase("!shutdown")) {
                e.getJDA().shutdownNow();
            } else if (msg.equalsIgnoreCase("!disablesort")) {
                disablesort = true;
                sendToMe("Done!");
            } else if (msg.equalsIgnoreCase("!disablestrike")) {
                PickCommand.isEnabled = false;
                sendToMe("Done!");
            } else if (msg.startsWith("!ban ")) {
                Guild g = e.getJDA().getGuildById(msg.split(" ")[1]);
                g.ban(msg.split(" ")[2], 0).queue();
            } else if (msg.equalsIgnoreCase("!updatedatabase")) {
                loadJSONFiles();
                sendToMe("Done!");
            } else if (msg.equalsIgnoreCase("!ej")) {
                emolgajson = load("./emolgadata.json");
                sendToMe("Done!");
            } else if (msg.equalsIgnoreCase("!updatestats")) {
                ArrayList<String> statorder = new ArrayList<>();
                JSONObject obj = getStatisticsJSON();
                if (!obj.getString("order").equals(""))
                    statorder.addAll(Arrays.asList(obj.getString("order").split(",")));
                int games = obj.getInt("games");
                JSONObject bst = getEmolgaJSON().getJSONObject("BST");
                String sid = bst.getString("sid");
                List<List<Object>> statsend = new ArrayList<>();
                List<List<Object>> newmons = new ArrayList<>();
                for (String s : statorder) {
                    JSONObject o = obj.getJSONObject(s);
                    int urate = o.getInt("usagerate");
                    int winrate = o.getInt("winrate");
                    int kills = o.getInt("kills");
                    int deaths = o.getInt("deaths");
                    System.out.println("s = " + s);
                    statsend.add(Arrays.asList(urate, divAndRound(urate, games * 2, true), winrate, divAndRound(winrate, urate, true), kills, divAndRound(kills, urate, false), deaths, divAndRound(deaths, urate, false), kills - deaths));
                    newmons.add(Arrays.asList(s, "=IMAGE(\"" + getSugiLink(s) + "\")"));
                }
                for (List<Object> objects : statsend) {
                    System.out.println(objects);
                }
                Google.updateRequest(sid, "Statistiken!C3", statsend, true, false);
                Google.updateRequest(sid, "Statistiken!A3", newmons, false, false);
            } else if (msg.equalsIgnoreCase("!updategiveaways")) {
                Giveaway.giveaways.clear();
                if (emolgajson.has("giveaways")) {
                    JSONArray arr = emolgajson.getJSONArray("giveaways");
                    for (Object o : arr) {
                        JSONObject obj = (JSONObject) o;
                        new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"));
                    }
                }
                sendToMe("Done!");
            } else if (msg.equalsIgnoreCase("!sortbst")) {
                sortBST();
                sendToMe("Done!");
            } else if (msg.startsWith("!style")) {
                Guild guild = e.getJDA().getGuildById("709877545708945438");
                Emote love = guild.getEmoteById("710842233712017478");
                Emote beep = guild.getEmoteById("745355018676469844");
                JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup");
                if (!league.has("doc")) return;
                String sid = league.getJSONObject("doc").getString("sid");
                Message message = guild.getTextChannelById("749194448507764766").sendMessage(split[2] + " (" + love.getAsMention() + ") oder " + split[4] + " (" + beep.getAsMention() + ")?").complete();
                message.addReaction(love).queue();
                message.addReaction(beep).queue();
                if (!json.has("style")) json.put("style", new JSONArray());
                JSONArray style = json.getJSONArray("style");
                JSONObject obj = new JSONObject();
                obj.put("uid1", split[1]);
                obj.put("uid2", split[3]);
                obj.put("mid", message.getId());
                obj.put("timer", System.currentTimeMillis() + 86400000 + "");
                style.put(obj);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        JSONObject json = getEmolgaJSON();
                        JSONArray style = json.getJSONArray("style");
                        JSONObject league = json.getJSONObject("drafts").getJSONObject("Wooloo Cup");
                        int p1 = -1;
                        int p2 = -1;
                        for (MessageReaction reaction : message.getReactions()) {
                            if (reaction.getReactionEmote().isEmoji()) continue;
                            Emote emote = reaction.getReactionEmote().getEmote();
                            List<Member> list = reaction.retrieveUsers().stream().map(u -> guild.retrieveMember(u).complete()).filter(mem -> mem.getRoles().contains(guild.getRoleById("742650292004454483"))).collect(Collectors.toList());
                            if (emote.getId().equals(love.getId())) {
                                p1 = list.size();
                            } else if (emote.getId().equals(beep.getId())) {
                                p2 = list.size();
                            }
                        }
                        String uid;
                        if (p1 > p2) {
                            uid = split[1];
                        } else if (p1 < p2) {
                            uid = split[3];
                        } else {
                            uid = new Random().nextInt(2) == 0 ? split[1] : split[3];
                        }
                        Google.updateRequest(sid,
                                "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51),
                                Collections.singletonList(Collections.singletonList(
                                        Integer.parseInt((String) Google.get(sid, "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51), false, false).get(0).get(0) + 1)
                                )), false, false);
                        int index = -1;
                        for (int i = 0; i < style.length(); i++) {
                            JSONObject obj = style.getJSONObject(i);
                            if (obj.getString("mid").equals(message.getId())) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1)
                            style.remove(index);
                        saveEmolgaJSON();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        sortWooloo(sid, league);
                    }
                }, 86400000);
                saveEmolgaJSON();
            } else if (msg.startsWith("!del")) {
                e.getJDA().getTextChannelById(split[1]).deleteMessageById(split[2]).queue();
            } else if (msg.startsWith("!as")) {
                as = e.getJDA().getGuildById(split[1]).retrieveMemberById(split[2]).complete();
                sendToMe("Done! **As** is now " + as.getAsMention());
            } else if (msg.equalsIgnoreCase("!checkbst")) {
                checkBST = !checkBST;
                sendToMe("CheckBST: " + checkBST);
            } else if (msg.startsWith("!sortzbs")) {
                JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + split[1]);
                sortZBS(league.getJSONObject("doc").getString("sid"), "Liga " + split[1], league);
            } else if (msg.equalsIgnoreCase("!initdrafts")) {
                JDA jda = e.getJDA();
                new Draft(jda.getTextChannelById("765218911800918026"), "Coach", "765235829756002335", true);
                new Draft(jda.getTextChannelById("765219001176424539"), "PK1", "765235875252535316", true);
                new Draft(jda.getTextChannelById("765219049142878228"), "PK2", "765235910513000530", true);
                new Draft(jda.getTextChannelById("765219098216759317"), "PK3", "765235944437448704", true);
            }
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
                new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"));
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
                    Emote love = guild.getEmoteById("710842233712017478");
                    Emote beep = guild.getEmoteById("745355018676469844");
                    String uid1 = obj.getString("uid1");
                    String uid2 = obj.getString("uid2");
                    JSONObject json = getEmolgaJSON();
                    JSONObject league = json.getJSONObject("drafts").getJSONObject("Wooloo Cup");
                    String sid = league.getJSONObject("doc").getString("sid");
                    Message message = guild.getTextChannelById("749194448507764766").retrieveMessageById(obj.getString("mid")).complete();
                    int p1 = -1;
                    int p2 = -1;
                    for (MessageReaction reaction : message.getReactions()) {
                        if (reaction.getReactionEmote().isEmoji()) continue;
                        Emote emote = reaction.getReactionEmote().getEmote();
                        List<Member> list = reaction.retrieveUsers().stream().map(u -> guild.retrieveMember(u).complete()).filter(mem -> mem.getRoles().contains(guild.getRoleById("742650292004454483"))).collect(Collectors.toList());
                        if (emote.getId().equals(love.getId())) {
                            p1 = list.size();
                        } else if (emote.getId().equals(beep.getId())) {
                            p2 = list.size();
                        }
                    }
                    String uid;
                    if (p1 > p2) {
                        uid = uid1;
                    } else if (p1 < p2) {
                        uid = uid2;
                    } else {
                        uid = new Random().nextInt(2) == 0 ? uid1 : uid2;
                    }
                    Google.updateRequest(sid,
                            "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51),
                            Collections.singletonList(Collections.singletonList(
                                    Integer.parseInt((String) Google.get(sid, "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51), false, false).get(0).get(0)) + 1
                            )), false, false);
                    int index = -1;
                    for (int i = 0; i < style.length(); i++) {
                        JSONObject obj = style.getJSONObject(i);
                        if (obj.getString("mid").equals(message.getId())) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1)
                        style.remove(index);
                    saveEmolgaJSON();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    sortWooloo(sid, league);
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
        if (e.getMessageId().equals("759407279094628383")) {
            Guild g = e.getGuild();
            if (e.getReaction().getReactionEmote().isEmoji()) return;
            String name = e.getReaction().getReactionEmote().getEmote().getName();
            Member mem = e.getMember();
            if (name.equalsIgnoreCase("morpeko")) {
                Role r = g.getRoleById("719928482544484352");
                if (mem.getRoles().contains(r)) {
                    g.removeRoleFromMember(mem, r).queue();
                } else {
                    g.addRoleToMember(mem, r).queue();
                }
                e.getChannel().removeReactionById(e.getMessageId(), g.getEmotesByName("morpeko", true).get(0), mem.getUser()).queue();
            } else if (name.equalsIgnoreCase("gasm")) {
                Role r = g.getRoleById("719928323731357696");
                if (mem.getRoles().contains(r)) {
                    g.removeRoleFromMember(mem, r).queue();
                } else {
                    g.addRoleToMember(mem, r).queue();
                }
                e.getChannel().removeReactionById(e.getMessageId(), g.getEmotesByName("gasm", true).get(0), mem.getUser()).queue();
            } else if (name.equalsIgnoreCase("gasm2")) {
                Role r = g.getRoleById("719928663935680644");
                if (mem.getRoles().contains(r)) {
                    g.removeRoleFromMember(mem, r).queue();
                } else {
                    g.addRoleToMember(mem, r).queue();
                }
                e.getChannel().removeReactionById(e.getMessageId(), g.getEmotesByName("gasm2", true).get(0), mem.getUser()).queue();
            }
        }
        if (e.getMessageId().equals("755331617970454558")) {
            e.getReaction().clearReactions().queue();
        }
        Optional<Message> op = helps.stream().filter(m -> m.getId().equals(e.getMessageId())).findFirst();
        if (op.isPresent() && e.getReaction().getReactionEmote().isEmoji()) {
            e.getReaction().removeReaction(e.getUser()).queue();
            Message m = op.get();
            String emoji = e.getReactionEmote().getAsCodepoints();
            if (emoji.equals("U+25c0U+fe0f")) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Commands").setColor(java.awt.Color.CYAN);
                builder.setDescription(getHelpDescripion(e.getGuild(), e.getMember()));
                builder.setColor(java.awt.Color.CYAN);
                addReactions(m, e.getMember());
                m.editMessage(builder.build()).queue();
            }
            CommandCategory c = null;
            switch (emoji) {
                case "U+1f1f5":
                    c = CommandCategory.Pokemon;
                    break;
                case "U+1f1f2":
                    c = CommandCategory.Music;
                    break;
                case "U+1f1e9":
                    c = CommandCategory.Draft;
                    break;
                case "U+1f1f6":
                    c = CommandCategory.Dexquiz;
                    break;
                case "U+1f1e7":
                    if (allowsCategory(e.getGuild(), CommandCategory.BS)) {
                        c = CommandCategory.BS;
                        break;
                    }
                case "U+1f1fb":
                    if (allowsCategory(e.getGuild(), CommandCategory.Verschiedenes)) {
                        c = CommandCategory.Verschiedenes;
                        break;
                    }
                case "U+1f1e6":
                    if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                        c = CommandCategory.Admin;
                        break;
                    }
                default:
                    break;
            }
            if (c != null) {
                List<Command> list = getWithCategory(c, e.getGuild(), e.getMember());
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(c.toString());
                builder.setColor(java.awt.Color.CYAN);
                builder.setDescription(list.stream().map(cmd -> cmd.getHelp(e.getGuild())).collect(Collectors.joining("\n")) + "\n\u25c0\ufe0f Zurück zur Übersicht");
                m.editMessage(builder.build()).queue();
            }
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
            if (gid.equals("712035338846994502")) {
                if (g.getSelfMember().canInteract(member)) {
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
            if (tco.getId().equals("759712094223728650") || tco.getId().equals("759734608773775360")) {
                JSONObject bst = getEmolgaJSON().getJSONObject("BST");
                String raw = m.getContentRaw();
                System.out.println("raw = " + raw);
                ArrayList<String> list = new ArrayList<>(Arrays.asList(msg.split("\n")));
                int gameday = bst.getInt("gameday");
                ArrayList<String> gdl = new ArrayList<>(Arrays.asList(bst.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";")));
                String p1;
                if (as != null && member.getId().equals("175910318608744448"))
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
                y1 = y1 * 17 + 3;
                x2 = x2 * 10 + 3;
                y2 = y2 * 17 + 3;
                if (y1 == 71) x1 += 10;
                if (y2 == 71) x2 += 10;
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
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1) + y1, Collections.singletonList(Arrays.asList(stat1.getInt("wins"), stat1.getInt("looses"))), false, false);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2) + y2, Collections.singletonList(Arrays.asList(stat2.getInt("wins"), stat2.getInt("looses"))), false, false);
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
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1 + 5) + (y1 + gameday + 3), Collections.singletonList(Arrays.asList(k1sum, d1sum)), false, false);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2 + 5) + (y2 + gameday + 3), Collections.singletonList(Arrays.asList(d1sum, k1sum)), false, false);
                int ip = gdl.indexOf(gdl.stream().filter(s -> s.contains(p1)).collect(Collectors.joining("")));
                if (r1.size() == 3) r1.add(Collections.emptyList());
                if (r2.size() == 3) r2.add(Collections.emptyList());
                r1.add(Collections.emptyList());
                r1.add(Collections.singletonList(k1sum));
                r1.add(Collections.singletonList(d1sum));
                r2.add(Collections.emptyList());
                r2.add(Collections.singletonList(d1sum));
                r2.add(Collections.singletonList(k1sum));
                if (gdl.get(ip).split(":")[0].equals(p1)) {
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 4) + (ip * 8 + 7), Collections.singletonList(Collections.singletonList(stat1.getInt("wins") + "-" + stat1.getInt("looses"))), false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5) + (ip * 8 + 7), Collections.singletonList(Collections.singletonList(stat2.getInt("wins") + "-" + stat2.getInt("looses"))), false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 3) + (ip * 8 + 3), r1, false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 1) + (ip * 8 + 3), r2, false, false);
                } else {
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 4) + (ip * 8 + 7), Collections.singletonList(Collections.singletonList(stat2.getInt("wins") + "-" + stat2.getInt("looses"))), false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5) + (ip * 8 + 7), Collections.singletonList(Collections.singletonList(stat1.getInt("wins") + "-" + stat1.getInt("looses"))), false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 3) + (ip * 8 + 3), r2, false, false);
                    Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 1) + (ip * 8 + 3), r1, false, false);
                }
                List<Object> get1 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + 2) + ":" + getAsXCoord(x1) + (y1 + 2), false, false).get(0);
                List<Object> get2 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + 2) + ":" + getAsXCoord(x2) + (y2 + 2), false, false).get(0);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + 2), Collections.singletonList(Arrays.asList(Integer.parseInt((String) get1.get(0)) + wins1.size(), Integer.parseInt((String) get1.get(1)) + (results.size() - wins1.size()))), false, false);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + 2), Collections.singletonList(Arrays.asList(Integer.parseInt((String) get2.get(0)) + (results.size() - wins1.size()), Integer.parseInt((String) get1.get(1)) + wins1.size())), false, false);
                List<List<Object>> send1 = new ArrayList<>();
                send1.add(p1m);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + gameday + 3), send1, false, false);
                List<List<Object>> send2 = new ArrayList<>();
                send2.add(p2m);
                Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + gameday + 3), send2, false, false);
                List<List<Object>> statsend = new ArrayList<>();
                int games = obj.getInt("games");
                for (String s : statorder) {
                    JSONObject o = obj.getJSONObject(s);
                    int urate = o.getInt("usagerate");
                    int winrate = o.getInt("winrate");
                    int kills = o.getInt("kills");
                    int deaths = o.getInt("deaths");
                    System.out.println("s = " + s);
                    statsend.add(Arrays.asList(urate, divAndRound(urate, games * 2, true), winrate, divAndRound(winrate, urate, true), kills, divAndRound(kills, urate, false), deaths, divAndRound(deaths, urate, false), kills - deaths));
                }
                Google.updateRequest(sid, "Statistiken!C3", statsend, true, false);
                System.out.println("statorder1 = " + statorder);
                System.out.println("origstatorder3 = " + statorder);
                statorder.removeAll(origstatorder);
                System.out.println("statorder2 = " + statorder);
                List<List<Object>> newmons = new ArrayList<>();
                for (String s : statorder) {
                    newmons.add(Arrays.asList(s, "=IMAGE(\"" + getSugiLink(s) + "\")"));
                }
                Google.updateRequest(sid, "Statistiken!A" + (origstatorder.size() + 3), newmons, false, false);
                System.out.println("Updating...");
                sortBST();
                return;
            }
            if (tco.getId().equals("743471003220443226") && !member.getUser().isBot()) {
                e.getJDA().retrieveUserById("574949229668335636").complete().openPrivateChannel().complete().sendMessage(msg).queue();
                return;
            }
            check(e);
            if (tco.getId().equals("758198459563114516")) {
                g.addRoleToMember(member, g.getRoleById("758254829885456404")).queue();
            }
            if (m.getMentionedMembers().size() == 1) {
                if (m.getMentionedMembers().get(0).getId().equals("723829878755164202") && !e.getAuthor().isBot()) {
                    help(tco, member);
                }
            }
            if ((tco.getId().equals("712612442622001162") || tco.getId().equals("724034089891397692")) && m.getAttachments().size() > 0 && !meid.equals("501445773985316865")) {
                tco.sendMessage("Gz!").queue();
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
                         * Pokemon Fan: 715247650018164826
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
                        e.getJDA().retrieveUserById("175910318608744448").complete().openPrivateChannel().complete().sendMessage("Fehler bei Level Up!").queue();
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
                    tco.sendMessage(member.getAsMention() + " hat das Pokemon erraten! Es war " + mon + "!").queue();
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
                        tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queue();
                    } catch (IOException | InterruptedException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
            JSONObject json = getEmolgaJSON();
            JSONObject analyse = json.getJSONObject("analyse");
            if (analyse.keySet().contains(tco.getId())) {
                if (msg.contains("https://")) {
                    String url = null;
                    for (String s : msg.split("\n")) {
                        //System.out.println("s = " + s);
                        if (!s.contains("https://replay.pokemonshowdown.com")) continue;
                        url = s.substring(s.indexOf("https://"), s.indexOf(" ", s.indexOf("https://") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("https://") + 1));
                        break;
                    }
                    if (url != null) {
                        System.out.println(url);
                        Player[] game = Analysis.analyse(url);
                        System.out.println("Analysed!");
                        if (game == null) {
                            tco.getGuild().getTextChannelById(analyse.getString(tco.getId())).sendMessage("Da in einem der beiden Teams ein Zoroark ist, kann ich das Ergebnis nicht bestimmen! Trage die Ergebnisse bitte selber ein!").queue();
                            return;
                        }
                        int deadP1 = 0;
                        int deadP2 = 0;
                        StringBuilder t1 = new StringBuilder();
                        StringBuilder t2 = new StringBuilder();
                        for (SDPokemon p : game[0].getMons()) {//Hallo Dieter\r\nTest\r\nDrei\r\nVier\r\nF\u00FCnf\r\nSechs
                            if (p.isDead()) deadP1++;
                        }
                        for (SDPokemon p : game[1].getMons()) {
                            if (p.isDead()) deadP2++;
                        }

                        String winloose = (6 - deadP1 - (6 - game[0].getMons().size())) + ":" + (6 - deadP2 - (6 - game[1].getMons().size()));
                        boolean p1wins = 6 - deadP1 > 0;
                        HashMap<String, String> kills = new HashMap<>();
                        HashMap<String, String> deaths = new HashMap<>();
                        boolean spoiler = false;
                        if (json.has("spoiler")) {
                            spoiler = json.getJSONArray("spoiler").toList().contains(tco.getGuild().getId());
                        }
                        if (msg.contains("709877545708945438")) gid = "709877545708945438";
                        if (msg.contains("747357029714231299")) gid = "747357029714231299";
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
                        String str;
                        if (spoiler) {
                            str = u1 + " ||" + winloose + "|| " + u2 + "\n\n" + game[0].getNickname() + ":\n" + t1.toString()
                                    + "\n" + game[1].getNickname() + ": " + "\n" + t2.toString();
                        } else {
                            str = u1 + " " + winloose + " " + u2 + "\n\n" + game[0].getNickname() + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1.toString()
                                    + "\n" + game[1].getNickname() + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2.toString();
                        }
                        tco.getGuild().getTextChannelById(analyse.getString(tco.getId())).sendMessage(str).queue();
                        if (!gid.equals("518008523653775366") && !gid.equals("447357526997073930") && !gid.equals("709877545708945438") && !gid.equals("747357029714231299"))
                            return;
                        JSONObject drafts = json.getJSONObject("drafts");
                        if (!json.getJSONObject("showdown").has(gid)) return;
                        JSONObject showdown = json.getJSONObject("showdown").getJSONObject(gid);
                        System.out.println("u1 = " + u1);
                        System.out.println("u2 = " + u2);
                        String uid1 = null;
                        String uid2 = null;
                        for (String s : showdown.keySet()) {
                            if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                            if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
                        }
                        if (uid1 == null || uid2 == null) return;
                        if (gid.equals("518008523653775366") || msg.contains("518008523653775366")) {
                            JSONObject league = null;
                            for (String s : drafts.keySet()) {
                                if (!drafts.getJSONObject(s).has("order")) continue;
                                if (Arrays.asList(drafts.getJSONObject(s).getJSONObject("order").getString("1").split(",")).contains(uid1))
                                    league = drafts.getJSONObject(s);
                            }
                            if (league != null) {
                                if (!league.has("doc")) return;
                                String sid = league.getJSONObject("doc").getString("sid");
                                int gameday = getGameDay(league, uid1, uid2);
                                if (gameday == -1) {
                                    System.out.println("GAMEDAY -1");
                                    return;
                                }
                                ArrayList<String> users = new ArrayList<>(Arrays.asList(uid1, uid2));
                                int i = 0;

                                for (String uid : users) {
                                    int index = Arrays.asList(league.getString("table").split(",")).indexOf(uid);
                                    char c = (char) (70 + (gameday - 1) * 2);
                                    String range = "Spielplan [erweitert]!" + c + (index * 14 + 3);
                                    boolean console = msg.contains("--console");
                                    //System.out.println("Range: " + range + ":" + c + (index * 14 + 14));
                                    //System.out.println("Gameday: " + gameday);
                                    List<List<Object>> check;
                                    try {
                                        check = Google.get(sid, range + ":" + c + (index * 14 + 14), false, false);
                                    } catch (IllegalArgumentException IllegalArgumentException) {
                                        IllegalArgumentException.printStackTrace();
                                        return;
                                    }
                                    //System.out.println("check = " + check);
                                    if (check != null && !console)
                                        for (List<Object> objects : check) {
                                            if (!(objects.size() > 0)) {
                                                System.out.println("ALREADY IN DOC!");
                                                return;
                                            }
                                        }
                                    ArrayList<String> picks = new ArrayList<>(Arrays.asList(league.getJSONObject("picks").getString(uid).split(",")));
                                    List<List<Object>> list = new ArrayList<>();
                                    for (String pick : picks) {
                                        list.add(Arrays.asList(getNumber(kills, pick), getNumber(deaths, pick)));
                                    }
                                    List<List<Object>> get;
                                    try {
                                        get = Google.get(sid, "Spielplan [erweitert]!D" + (index * 14 + 1) + ":E" + (index * 14 + 1), false, false);
                                    } catch (IllegalArgumentException IllegalArgumentException) {
                                        IllegalArgumentException.printStackTrace();
                                        return;
                                    }
                                    int win = Integer.parseInt((String) get.get(0).get(0));
                                    int loose = Integer.parseInt((String) get.get(0).get(1));
                                    if (game[i].isWinner()) {
                                        win++;
                                        if (!league.has("results"))
                                            league.put("results", new JSONObject());
                                        league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                                    } else loose++;
                                    if (console) {
                                        System.out.println(list.toString());
                                    } else {
                                        try {
                                            Google.updateRequest(sid, range, list, false, false);
                                            Google.updateRequest(sid, "Spielplan [erweitert]!D" + (index * 14 + 1), Collections.singletonList(Arrays.asList(win, loose)), false, false);
                                        } catch (IllegalArgumentException IllegalArgumentException) {
                                            IllegalArgumentException.printStackTrace();

                                        }
                                    }
                                    i++;
                                }
                                saveEmolgaJSON();
                                if (disablesort) return;
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                }
                                List<List<Object>> formula;
                                List<List<Object>> points;
                                try {
                                    formula = Google.get(sid, "Tabelle!B3:I12", true, false);
                                    points = Google.get(sid, "Tabelle!C3:J12", false, false);
                                } catch (IllegalArgumentException IllegalArgumentException) {
                                    IllegalArgumentException.printStackTrace();
                                    return;
                                }
                                List<List<Object>> orig = new ArrayList<>(points);
                                JSONObject finalLeague = league;
                                points.sort((o1, o2) -> {
                                    if (Integer.parseInt((String) o1.get(1)) != Integer.parseInt((String) o2.get(1))) {
                                        return Integer.compare(Integer.parseInt((String) o1.get(1)), Integer.parseInt((String) o2.get(1)));
                                    }
                                    if (Integer.parseInt((String) o1.get(7)) != Integer.parseInt((String) o2.get(7))) {
                                        return Integer.compare(Integer.parseInt((String) o1.get(7)), Integer.parseInt((String) o2.get(7)));
                                    }
                                    if (Integer.parseInt((String) o1.get(5)) != Integer.parseInt((String) o2.get(5))) {
                                        return Integer.compare(Integer.parseInt((String) o1.get(5)), Integer.parseInt((String) o2.get(5)));
                                    }
                                    //System.out.println(o1.get(0) + " oberhalb von " + o2.get(0) + "?");
                                    if (finalLeague.has("results")) return -1;
                                    JSONObject results = finalLeague.getJSONObject("results");
                                    String n1 = json.getJSONObject("docnames").getString((String) o1.get(0));
                                    String n2 = json.getJSONObject("docnames").getString((String) o2.get(0));
                                    if (results.has(n1 + ":" + n2)) {
                                        return results.getString(n1 + ":" + n2).equals(n1) ? 1 : -1;
                                    }
                                    if (results.has(n2 + ":" + n1)) {
                                        return results.getString(n2 + ":" + n1).equals(n1) ? 1 : -1;
                                    }
                                    return -1;
                                });
                                Collections.reverse(points);
                                //System.out.println(points);
                                Spreadsheet s;
                                try {
                                    s = Google.getSheetData(sid, "Tabelle!B3:B12", false);
                                } catch (IllegalArgumentException IllegalArgumentException) {
                                    IllegalArgumentException.printStackTrace();
                                    return;
                                }
                                Sheet sheet = s.getSheets().get(0);
                                ArrayList<CellFormat> formats = new ArrayList<>();
                                for (RowData rowDatum : sheet.getData().get(0).getRowData()) {
                                    formats.add(rowDatum.getValues().get(0).getEffectiveFormat());
                                }
                                HashMap<Integer, List<Object>> valmap = new HashMap<>();
                                HashMap<Integer, CellFormat> formap = new HashMap<>();
                                HashMap<Integer, List<Object>> namap = new HashMap<>();
                                i = 0;
                                for (List<Object> objects : orig) {
                                    List<Object> list = formula.get(i);
                                    Object logo = list.remove(0);
                                    Object name = list.remove(0);
                                    list.remove(0);
                                    list.remove(0);
                                    int index = points.indexOf(objects);
                                    valmap.put(index, list);
                                    formap.put(index, formats.get(i));
                                    namap.put(index, Arrays.asList(logo, name));
                                    i++;
                                }
                                List<List<Object>> senddata = new ArrayList<>();
                                List<List<Object>> sendname = new ArrayList<>();
                                for (int j = 0; j < 10; j++) {
                                    senddata.add(valmap.get(j));
                                    sendname.add(namap.get(j));
                                }
                                try {
                                    Google.updateRequest(sid, "Tabelle!F3", senddata, false, false);
                                    Google.updateRequest(sid, "Tabelle!B3", sendname, false, false);
                                } catch (IllegalArgumentException IllegalArgumentException) {
                                    IllegalArgumentException.printStackTrace();
                                    return;
                                }
                                for (int j = 0; j < 10; j++) {
                                    Request request = new Request();
                                    request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                                            .setValues(getXTimes(new CellData()
                                                    .setUserEnteredFormat(new CellFormat()
                                                            .setBackgroundColor(formap.get(j).getBackgroundColor()).setTextFormat(formap.get(j).getTextFormat().setFontSize(11).setFontFamily("Oswald"))), 9))))
                                            .setFields("UserEnteredFormat(BackgroundColor,TextFormat)").setRange(new GridRange().setSheetId(553533374).setStartRowIndex(j + 2).setEndRowIndex(j + 3).setStartColumnIndex(1).setEndColumnIndex(10)));
                                    try {
                                        Google.batchUpdateRequest(sid, request, false);
                                    } catch (IllegalArgumentException IllegalArgumentException) {
                                        IllegalArgumentException.printStackTrace();
                                        return;
                                    }
                                }
                            }
                        } else if (gid.equals("709877545708945438") || msg.contains("709877545708945438")) {
                            Guild guild = e.getJDA().getGuildById("709877545708945438");
                            Emote love = guild.getEmoteById("710842233712017478");
                            Emote beep = guild.getEmoteById("745355018676469844");
                            JSONObject league = drafts.getJSONObject("Wooloo Cup");
                            if (!league.has("doc")) return;
                            String sid = league.getJSONObject("doc").getString("sid");
                            String finalUid1 = uid1;
                            String finalUid2 = uid2;
                            Message message = guild.getTextChannelById("749194448507764766").sendMessage(u1 + " (" + love.getAsMention() + ") oder " + u2 + " (" + beep.getAsMention() + ")?").complete();
                            message.addReaction(love).queue();
                            message.addReaction(beep).queue();
                            if (!json.has("style")) json.put("style", new JSONArray());
                            JSONArray style = json.getJSONArray("style");
                            JSONObject obj = new JSONObject();
                            obj.put("uid1", finalUid1);
                            obj.put("uid2", finalUid2);
                            obj.put("mid", message.getId());
                            obj.put("timer", System.currentTimeMillis() + 86400000 + "");
                            style.put(obj);
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    JSONObject json = getEmolgaJSON();
                                    JSONArray style = json.getJSONArray("style");
                                    JSONObject league = json.getJSONObject("drafts").getJSONObject("Wooloo Cup");
                                    int p1 = -1;
                                    int p2 = -1;
                                    for (MessageReaction reaction : message.getReactions()) {
                                        if (reaction.getReactionEmote().isEmoji()) continue;
                                        Emote emote = reaction.getReactionEmote().getEmote();
                                        List<Member> list = reaction.retrieveUsers().stream().map(u -> guild.retrieveMember(u).complete()).filter(mem -> mem.getRoles().contains(guild.getRoleById("742650292004454483"))).collect(Collectors.toList());
                                        if (emote.getId().equals(love.getId())) {
                                            p1 = list.size();
                                        } else if (emote.getId().equals(beep.getId())) {
                                            p2 = list.size();
                                        }
                                    }
                                    String uid;
                                    if (p1 > p2) {
                                        uid = finalUid1;
                                    } else if (p1 < p2) {
                                        uid = finalUid2;
                                    } else {
                                        uid = new Random().nextInt(2) == 0 ? finalUid1 : finalUid2;
                                    }
                                    Google.updateRequest(sid,
                                            "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51),
                                            Collections.singletonList(Collections.singletonList(
                                                    Integer.parseInt((String) Google.get(sid, "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(finalUid1) + 51), false, false).get(0).get(0) + 1)
                                            )), false, false);
                                    int index = -1;
                                    for (int i = 0; i < style.length(); i++) {
                                        JSONObject obj = style.getJSONObject(i);
                                        if (obj.getString("mid").equals(message.getId())) {
                                            index = i;
                                            break;
                                        }
                                    }
                                    if (index != -1)
                                        style.remove(index);
                                    saveEmolgaJSON();
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException interruptedException) {
                                        interruptedException.printStackTrace();
                                    }
                                    sortWooloo(sid, league);
                                }
                            }, 86400000);
                            saveEmolgaJSON();
                            ArrayList<String> users = new ArrayList<>(Arrays.asList(uid1, uid2));
                            int i = 0;
                            for (String uid : users) {
                                int index = Arrays.asList(league.getString("table").split(",")).indexOf(uid);
                                String range = "Teamübersicht!"
                                        + (index < 4 ? (char) (index * 6 + 68) : (index > 7 ? (char) ((index - 6) * 6 + 68) : (char) ((index - 4) * 6 + 68)))
                                        + (index < 4 ? 7 : (index > 7 ? 41 : 24))
                                        + ":"
                                        + (index < 4 ? (char) (index * 6 + 69) : (index > 7 ? (char) ((index - 6) * 6 + 69) : (char) ((index - 4) * 6 + 69)))
                                        + (index < 4 ? 18 : (index > 7 ? 52 : 35));
                                //boolean console = msg.contains("--console");
                                ArrayList<String> picks = new ArrayList<>(Arrays.asList(league.getJSONObject("picks").getString(uid).split(",")));
                                List<List<Object>> list = new ArrayList<>();
                                List<List<Object>> get = Google.get(sid, range, false, false);
                                int x = 0;
                                for (String pick : picks) {
                                    String kill = getNumber(kills, pick);
                                    String death = getNumber(deaths, pick);
                                    list.add(Arrays.asList((kill.equals("") ? 0 : Integer.parseInt(kill)) + Integer.parseInt((String) get.get(x).get(0)), (death.equals("") ? 0 : Integer.parseInt(death)) + Integer.parseInt((String) get.get(x).get(1))));
                                    x++;
                                }
                                //List<List<Object>> get = Google.get(sid, "Spielplan [erweitert]!D" + (index * 14 + 1) + ":E" + (index * 14 + 1));
                                List<List<Object>> slist = Google.get(sid, "Teamübersicht!C" + (index + 40) + ":D" + (index + 40), false, false);
                                //System.out.println(uid);
                                //System.out.println("slist = " + slist);
                                int win = Integer.parseInt((String) slist.get(0).get(0));
                                int loose = Integer.parseInt((String) slist.get(0).get(1));
                                if (!league.has("results")) league.put("results", new JSONObject());
                                if (game[i].isWinner()) {
                                    win++;
                                    league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                                    //System.out.println("win = " + win);
                                } else {
                                    loose++;
                                    //System.out.println("loose = " + loose);
                                }
                                saveEmolgaJSON();
                                String urange = range.split(":")[0];
                                Google.updateRequest(sid, urange, list, false, false);
                                Google.updateRequest(sid, "Teamübersicht!C" + (index + 40), Collections.singletonList(Arrays.asList(win, loose)), true, false);
                                i++;
                            }
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            sortWooloo(sid, league);
                        } else if (gid.equals("747357029714231299")) {
                            int l = tco.getId().equals("747456375289806989") ? 1 : 2;
                            JSONObject league = drafts.getJSONObject("ZBSL" + l);
                            String sid = league.getJSONObject("doc").getString("sid");
                            int gameday = getGameDay(league, uid1, uid2);
                            if (gameday == -1) {
                                System.out.println("GAMEDAY -1");
                                return;
                            }
                            ArrayList<String> users = new ArrayList<>(Arrays.asList(uid1, uid2));
                            int i = 0;
                            for (String uid : users) {
                                int index = Arrays.asList(league.getString("table").split(",")).indexOf(uid);
                                //System.out.println("Range: " + range + ":" + c + (index * 14 + 14));
                                //System.out.println("Gameday: " + gameday);
                                //System.out.println("check = " + check);
                                ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                                List<List<Object>> get = Google.get(sid, "Liga " + l + "!B" + (index * 11 + 200) + ":D" + (index * 11 + 210), false, false);
                                List<List<Object>> list = new ArrayList<>();
                                int x = 0;
                                for (String pick : picks) {
                                    String kill = getNumber(kills, pick);
                                    String death = getNumber(deaths, pick);
                                    list.add(Arrays.asList((kill.equals("") ? 0 : Integer.parseInt(kill)) + Integer.parseInt((String) get.get(x).get(0)), (death.equals("") ? 0 : Integer.parseInt(death)) + Integer.parseInt((String) get.get(x).get(1)), Integer.parseInt((String) get.get(x).get(2)) + 1));
                                    x++;
                                }
                                List<List<Object>> getvic;
                                try {
                                    getvic = Google.get(sid, "Liga " + l + "!I" + (index + 3) + ":K" + (index + 3), false, false);
                                } catch (IllegalArgumentException IllegalArgumentException) {
                                    IllegalArgumentException.printStackTrace();
                                    return;
                                }
                                int win = Integer.parseInt((String) getvic.get(0).get(0));
                                int loose = Integer.parseInt((String) getvic.get(0).get(2));
                                if (game[i].isWinner()) {
                                    win++;
                                    if (!league.has("results"))
                                        league.put("results", new JSONObject());
                                    league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                                } else loose++;
                                try {
                                    Google.updateRequest(sid, "Liga " + l + "!B" + (index * 11 + 200), list, false, false);
                                    Google.updateRequest(sid, "Liga " + l + "!I" + (index + 3), Collections.singletonList(Arrays.asList(win, getvic.get(0).get(1), loose)), false, false);
                                } catch (IllegalArgumentException IllegalArgumentException) {
                                    IllegalArgumentException.printStackTrace();

                                }
                                i++;
                            }
                            sortZBS(sid, "Liga " + l, league);
                            saveEmolgaJSON();
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
