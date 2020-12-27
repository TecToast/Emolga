package de.tectoast.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.EmolgaMain;
import de.tectoast.utils.Constants;
import de.tectoast.utils.draft.Draft;
import de.tectoast.utils.draft.Tierlist;
import de.tectoast.utils.Giveaway;
import de.tectoast.utils.music.GuildMusicManager;
import de.tectoast.utils.RequestBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.commands.Command.*;

public class PrivateCommands {

    public static void fromPrivate(PrivateMessageReceivedEvent e) {
        execute(e.getJDA(), e.getChannel(), e.getMessage());
    }

    public static void fromGuild(GuildMessageReceivedEvent e) {
        execute(e.getJDA(), e.getChannel(), e.getMessage());
    }

    private static void execute(JDA jda, MessageChannel tco, Message message) {
        JSONObject json = getEmolgaJSON();
        String msg = message.getContentDisplay();
        String[] split = msg.split(" ");
        if (msg.startsWith("!timer")) {
            String name = msg.substring(7);
            List<Draft> list = Draft.drafts.stream().filter(d -> d.name.equals(name)).collect(Collectors.toList());
            if (list.size() == 0) {
                tco.sendMessage("Dieser draft existiert nicht!").queue();
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
            tco.sendMessage("Der Timer bei " + name + " wurde aktiviert!").queue();
        } else if (msg.startsWith("!updatetierlist")) {
            Tierlist.setup();
            tco.sendMessage("Die Tierliste wurde aktualisiert!").queue();
        } else if (msg.startsWith("!skip")) {
            Draft.drafts.stream().filter(draft -> draft.name.equals(msg.substring(6))).collect(Collectors.toList()).get(0).timer();
        } else if (msg.startsWith("!edit ")) {
            jda.getTextChannelById(split[1]).editMessageById(split[2], msg.substring(43)).queue();
        } else if (msg.startsWith("!send")) {
            System.out.println(message.getContentRaw());
            String s = message.getContentRaw().substring(24).replaceAll("\\\\", "");
            TextChannel tc = jda.getTextChannelById(split[1]);
            Guild g = tc.getGuild();
            for (ListedEmote emote : g.retrieveEmotes().complete()) {
                s = s.replace("<<" + emote.getName() + ">>", emote.getAsMention());
            }
            tc.sendMessage(s).queue();
        } else if (msg.startsWith("!doit")) {
            System.out.println(jda.getGuildById("709877545708945438").getRoles().stream().map(Role::getName).collect(Collectors.joining("\n")));
        } else if (msg.startsWith("!react")) {
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
            //System.out.println(m.getContentDisplay());
        } else if (msg.startsWith("!join")) {
            Guild g = jda.getGuildById(split[1]);
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
        } else if (msg.startsWith("!ban ")) {
            Guild g = jda.getGuildById(msg.split(" ")[1]);
            g.ban(msg.split(" ")[2], 0).queue();
        } else if (msg.equalsIgnoreCase("!updatedatabase")) {
            loadJSONFiles();
            tco.sendMessage("Done!").queue();
        } else if (msg.equalsIgnoreCase("!ej")) {
            emolgajson = load("./emolgadata.json");
            tco.sendMessage("Done!").queue();
        } else if (msg.equalsIgnoreCase("!updatestats")) {
            ArrayList<String> statorder = new ArrayList<>();
            JSONObject obj = getStatisticsJSON();
            if (!obj.getString("order").equals(""))
                statorder.addAll(Arrays.asList(obj.getString("order").split(",")));
            int games = obj.getInt("games");
            JSONObject bst = getEmolgaJSON().getJSONObject("BST");
            String sid = bst.getString("sid");
            List<List<Object>> statsend = new ArrayList<>();
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
            new RequestBuilder(sid).addAll("Statistiken!C3", statsend).addSingle("Statistiken!A1", games).execute();
        } else if (msg.equalsIgnoreCase("!updategiveaways")) {
            Giveaway.giveaways.clear();
            if (emolgajson.has("giveaways")) {
                JSONArray arr = emolgajson.getJSONArray("giveaways");
                for (Object o : arr) {
                    JSONObject obj = (JSONObject) o;
                    new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"), obj.has("role"));
                }
            }
            tco.sendMessage("Done!").queue();
        } else if (msg.equalsIgnoreCase("!sortbst")) {
            sortBST();
            tco.sendMessage("Done!").queue();
        } else if (msg.startsWith("!style")) {
            Guild guild = jda.getGuildById("709877545708945438");
            Emote love = guild.getEmoteById("710842233712017478");
            Emote beep = guild.getEmoteById("745355018676469844");
            JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup");
            if (!league.has("doc")) return;
            String sid = league.getJSONObject("doc").getString("sid");
            Message m = guild.getTextChannelById("749194448507764766").sendMessage(split[2] + " (" + love.getAsMention() + ") oder " + split[4] + " (" + beep.getAsMention() + ")?").complete();
            m.addReaction(love).queue();
            m.addReaction(beep).queue();
            if (!json.has("style")) json.put("style", new JSONArray());
            JSONArray style = json.getJSONArray("style");
            JSONObject obj = new JSONObject();
            obj.put("uid1", split[1]);
            obj.put("uid2", split[3]);
            obj.put("mid", m.getId());
            obj.put("timer", System.currentTimeMillis() + 86400000 + "");
            style.put(obj);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    woolooStyle(sid, m, split[1], split[3]);
                }
            }, 86400000);
            saveEmolgaJSON();
        } else if (msg.startsWith("!del ")) {
            jda.getTextChannelById(split[1]).deleteMessageById(split[2]).queue();
        } else if (msg.startsWith("!as ")) {
            as = jda.getGuildById(split[1]).retrieveMemberById(split[2]).complete();
            tco.sendMessage("Done! **As** is now " + as.getAsMention()).queue();
        } else if (msg.equalsIgnoreCase("!checkbst")) {
            checkBST = !checkBST;
            tco.sendMessage("CheckBST: " + checkBST).queue();
        } else if (msg.startsWith("!sortzbs")) {
            JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + split[1]);
            sortZBS(league.getJSONObject("doc").getString("sid"), "Liga " + split[1], league);
        } else if (msg.startsWith("!sortwooloo")) {
            JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup");
            sortWooloo(league.getJSONObject("doc").getString("sid"), league);
        } else if (msg.equalsIgnoreCase("!initdrafts")) {
            new Draft(jda.getTextChannelById("765218911800918026"), "Coach", "765235829756002335", true);
            new Draft(jda.getTextChannelById("765219001176424539"), "PK1", "765235875252535316", true);
            new Draft(jda.getTextChannelById("765219049142878228"), "PK2", "765235910513000530", true);
            new Draft(jda.getTextChannelById("765219098216759317"), "PK3", "765235944437448704", true);
        } else if (msg.startsWith("!troll")) {
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
        } else if (msg.startsWith("!updatetable")) {
            updateTable(getEmolgaJSON().getJSONObject("BlitzTurnier"), jda.getTextChannelById("771403849029386270"));
        } else if (msg.equalsIgnoreCase("!nextround")) {
            new Thread(() -> {
                JSONObject b = json.getJSONObject("BlitzTurnier");
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
        } else if (msg.equals("!levels")) {
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
        } else if (msg.equals("!generateorder")) {
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
        } else if(msg.equals("!testmuffin")) {
            Guild g = jda.getGuildById("745934535748747364");
            GuildMusicManager musicManager = getGuildAudioPlayer(g);
            playerManager.loadItem("./muffin.mp3", new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    play(g, musicManager, track, jda.getVoiceChannelById("783079191200530452"));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {

                }

                @Override
                public void noMatches() {

                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }
}
