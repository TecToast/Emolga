package de.Flori.Commands;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.youtube.model.SearchResult;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.Flori.Commands.Admin.*;
import de.Flori.Commands.BS.*;
import de.Flori.Commands.DexQuiz.DexquizCommand;
import de.Flori.Commands.DexQuiz.SolutionCommand;
import de.Flori.Commands.DexQuiz.TipCommand;
import de.Flori.Commands.Draft.*;
import de.Flori.Commands.Flo.GetIdsCommand;
import de.Flori.Commands.Flo.GiveMeAdminPermissionsCommand;
import de.Flori.Commands.Flo.GoinCommand;
import de.Flori.Commands.Giveaway.GcreateCommand;
import de.Flori.Commands.Music.*;
import de.Flori.Commands.Pokemon.*;
import de.Flori.Emolga.EmolgaMain;
import de.Flori.utils.Google;
import de.Flori.utils.Music.GuildMusicManager;
import de.Flori.utils.ReplayAnalyser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public abstract class Command {

    public static final String NOPERM = "Dafür hast du keine Berechtigung!";
    public static final File emolgadata = new File("./emolgadata.json");
    //protected static final String tradesid = "1KGpou63t5_V-9nZaIPt_LDKzBwsw-lDSOrn-p7QjbtY";
    public static ArrayList<Command> commands = new ArrayList<>();
    public static ArrayList<String> hazards = new ArrayList<>();
    public static ArrayList<String> recovery = new ArrayList<>();
    public static ArrayList<String> setup = new ArrayList<>();
    public static ArrayList<String> momentum = new ArrayList<>();
    public static ArrayList<String> flinch = new ArrayList<>();
    public static JSONObject wikijson;
    public static JSONObject datajson;
    public static JSONObject movejson;
    public static JSONObject spritejson;
    public static JSONObject shinyspritejson;
    public static JSONObject emolgajson;
    public static JSONObject huntjson;
    public static JSONObject statisticsjson;
    public static JSONObject ytjson;
    public static JSONObject leveljson;
    public static JSONObject tokens;
    public static AudioPlayerManager playerManager;
    //public static ArrayList<String> todel = new ArrayList<>();
    public static Map<Long, GuildMusicManager> musicManagers;
    public static ArrayList<Guild> chill = new ArrayList<>();
    public static ArrayList<Guild> deep = new ArrayList<>();
    public static ArrayList<Guild> music = new ArrayList<>();
    public static ConcurrentLinkedQueue<byte[]> bytes = new ConcurrentLinkedQueue<>();
    public static HashMap<CommandCategory, List<Command>> categorys = new HashMap<>();
    public static HashSet<Message> helps = new HashSet<>();
    public static HashMap<String, List<String>> emolgachannel = new HashMap<>();
    public static HashMap<String, String> serebiiex = new HashMap<>();
    public static HashMap<String, String> sdex = new HashMap<>();
    public static HashMap<String, Long> latestExp = new HashMap<>();
    public static boolean expEdited = false;
    public static HashMap<String, Double> expmultiplicator = new HashMap<>();
    public static HashMap<String, ReplayAnalyser> sdAnalyser = new HashMap<>();
    protected static String tradesid;
    protected static List<String> balls;
    protected static List<String> mons;
    public List<String> allowedGuilds;
    public boolean onlyAdmin = false;
    public HashSet<String> aliases = new HashSet<>();
    public HashMap<String, String> overrideHelp = new HashMap<>();
    public HashMap<String, List<String>> overrideChannel = new HashMap<>();
    protected boolean wip = false;
    protected String name;
    protected String help;
    protected CommandCategory category;
    protected boolean everywhere = false;

    public Command(String name, String help, CommandCategory category, boolean onlyAdmin, String... guilds) {
        this(name, help, category, guilds);
        this.onlyAdmin = onlyAdmin;
    }

    public Command(String name, String help, CommandCategory category, String... guilds) {
        this.name = name;
        this.help = help;
        this.category = category;
        if (category == CommandCategory.Admin) onlyAdmin = true;
        allowedGuilds = guilds == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(guilds));
        commands.add(this);
        if (!categorys.containsKey(category)) categorys.put(category, new ArrayList<>());
        categorys.get(category).add(this);
    }

    public static void loadPlaylist(final TextChannel channel, final String track, Member mem, String cm) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        System.out.println(track);
        playerManager.loadItemOrdered(musicManager, track, new AudioLoadResultHandler() {


            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack playlistTrack : playlist.getTracks()) {
                    play(channel.getGuild(), musicManager, playlistTrack, mem, channel);
                }
                channel.sendMessage(cm == null ? "Loaded playlist!" : cm).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Es wurde unter `" + track + "` nichts gefunden!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
                channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.getMessage()).queue();
            }
        });
    }

    public static void loadAndPlay(final TextChannel channel, final String track, Member mem, String cm) throws IllegalArgumentException {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        System.out.println(track);
        SearchResult s = Google.getVid(track, false);
        String url = "https://www.youtube.com/watch?v=" + s.getId().getVideoId();
        playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                //System.out.println("LOADED!");
                if (cm == null) {
                    long duration = musicManager.scheduler.queue.stream().mapToLong(AudioTrack::getDuration).sum() / 1000;
                    if (musicManager.player.getPlayingTrack() != null) {
                        duration += (musicManager.player.getPlayingTrack().getDuration() - musicManager.player.getPlayingTrack().getPosition());
                    }
                    EmbedBuilder b = new EmbedBuilder()
                            .setTitle(track.getInfo().title, "https://www.youtube.com/watch?v=" + s.getId().getVideoId())
                            .setAuthor("Added to queue", null, mem.getUser().getEffectiveAvatarUrl())
                            .addField("Channel", s.getSnippet().getChannelTitle(), true)
                            .addField("Song Duration", formatToTime(track.getDuration()), true)
                            .addField("Estimated time until playing", formatToTime(duration), true)
                            .addField("Position in queue", (musicManager.scheduler.queue.size() > 0 ? musicManager.scheduler.queue.size() + 1 : 0) + "", false)
                            .setThumbnail(s.getSnippet().getThumbnails().getMedium().getUrl());
                    channel.sendMessage(b.build()).queue();
                } else {
                    channel.sendMessage(cm).queue();
                }
                play(channel.getGuild(), musicManager, track, mem, channel);
            }


            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack, mem, channel);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Es wurde unter `" + track + "` nichts gefunden!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
                channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.getMessage()).queue();
            }
        });
    }

    public static int parseShortTime(String timestr) {
        timestr = timestr.toLowerCase();
        if (!timestr.matches("\\d{1,8}[smhd]?"))
            return -1;
        int multiplier = 1;
        switch (timestr.charAt(timestr.length() - 1)) {
            case 'd':
                multiplier *= 24;
            case 'h':
                multiplier *= 60;
            case 'm':
                multiplier *= 60;
            case 's':
                timestr = timestr.substring(0, timestr.length() - 1);
            default:
        }
        return multiplier * Integer.parseInt(timestr);
    }

    public static String secondsToTime(long timeseconds) {
        StringBuilder builder = new StringBuilder();
        int years = (int) (timeseconds / (60 * 60 * 24 * 365));
        if (years > 0) {
            builder.append("**").append(years).append("** ").append(pluralise(years, "Jahr", "Jahre")).append(", ");
            timeseconds = timeseconds % (60 * 60 * 24 * 365);
        }
        int weeks = (int) (timeseconds / (60 * 60 * 24 * 7));
        if (weeks > 0) {
            builder.append("**").append(weeks).append("** ").append(pluralise(weeks, "Woche", "Wochen")).append(", ");
            timeseconds = timeseconds % (60 * 60 * 24 * 7);
        }
        int days = (int) (timeseconds / (60 * 60 * 24));
        if (days > 0) {
            builder.append("**").append(days).append("** ").append(pluralise(days, "Tag", "Tage")).append(", ");
            timeseconds = timeseconds % (60 * 60 * 24);
        }
        int hours = (int) (timeseconds / (60 * 60));
        if (hours > 0) {
            builder.append("**").append(hours).append("** ").append(pluralise(hours, "Stunde", "Stunden")).append(", ");
            timeseconds = timeseconds % (60 * 60);
        }
        int minutes = (int) (timeseconds / (60));
        if (minutes > 0) {
            builder.append("**").append(minutes).append("** ").append(pluralise(minutes, "Minute", "Minuten")).append(", ");
            timeseconds = timeseconds % (60);
        }
        if (timeseconds > 0) {
            builder.append("**").append(timeseconds).append("** ").append(pluralise(timeseconds, "Sekunde", "Sekunden"));
        }
        String str = builder.toString();
        if (str.endsWith(", "))
            str = str.substring(0, str.length() - 2);
        if (str.equals(""))
            str = "**0** Sekunden";
        return str;
    }

    public static String pluralise(long x, String singular, String plural) {
        return x == 1 ? singular : plural;
    }

    public static int getGameDay(JSONObject league, String uid1, String uid2) {
        JSONObject battleorder = league.getJSONObject("battleorder");
        for (String s : battleorder.keySet()) {
            String str = battleorder.getString(s);
            if (str.contains(uid1 + ":" + uid2) || str.contains(uid2 + ":" + uid1)) return Integer.parseInt(s);
        }
        return -1;
    }

    public static ArrayList<String> getPicksAsList(JSONArray arr) {
        return arr.toList().stream().map(o -> (String) ((HashMap<?, ?>) o).get("name")).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void tempMute(TextChannel tco, Member mod, Member mem, int time, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht muten!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        if (g.getId().equals("712035338846994502"))
            g.addRoleToMember(mem, g.getRoleById("717297533294215258")).queue();
        else if (g.getId().equals("447357526997073930"))
            g.addRoleToMember(mem, g.getRoleById("761723664273899580")).queue();
        JSONObject json = getEmolgaJSON();
        if (!json.has("mutes")) json.put("mutes", new JSONArray());
        JSONArray arr = json.getJSONArray("mutes");
        JSONObject obj = new JSONObject();
        obj.put("mod", mod.getId());
        obj.put("user", mem.getId());
        obj.put("reason", reason);
        obj.put("guild", g.getId());
        long delay = System.currentTimeMillis() + time * 1000;
        obj.put("delay", delay);
        arr.put(obj);
        muteTimer(g, delay, time * 1000, mem.getId());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        saveEmolgaJSON();
    }

    public static void muteTimer(Guild g, long delay, long time, String mem) {
        JSONObject json = new JSONObject();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!json.has("mutes")) json.put("mutes", new JSONArray());
                JSONArray arr = json.getJSONArray("mutes");
                boolean success = false;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = (JSONObject) arr.get(i);
                    if (obj.getLong("delay") == delay) {
                        arr.remove(i);
                        success = true;
                    }
                }
                saveEmolgaJSON();
                System.out.println("Unmuted!");
                if (success) {
                    if (g.getId().equals("712035338846994502"))
                        g.removeRoleFromMember(mem, g.getRoleById("717297533294215258")).queue();
                    else if (g.getId().equals("447357526997073930"))
                        g.removeRoleFromMember(mem, g.getRoleById("761723664273899580")).queue();
                }
            }
        }, time);
    }

    public static void kick(TextChannel tco, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht kicken!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        g.kick(mem, reason).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gekickt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
    }

    public static void ban(TextChannel tco, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht bannen!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        g.ban(mem, 0, reason).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gebannt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
    }

    public static void mute(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht muten!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        if (g.getId().equals("712035338846994502"))
            g.addRoleToMember(mem, g.getRoleById("717297533294215258")).queue();
        else if (g.getId().equals("447357526997073930"))
            g.addRoleToMember(mem, g.getRoleById("761723664273899580")).queue();
        JSONObject json = getEmolgaJSON();
        if (!json.has("mutes")) json.put("mutes", new JSONArray());
        JSONArray arr = json.getJSONArray("mutes");
        JSONObject obj = new JSONObject();
        obj.put("mod", mod.getId());
        obj.put("user", mem.getId());
        obj.put("reason", reason);
        obj.put("guild", g.getId());
        arr.put(obj);
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        saveEmolgaJSON();
    }

    public static void unmute(TextChannel tco, Member mem) {
        JSONObject json = getEmolgaJSON();
        Guild g = tco.getGuild();
        if (g.getId().equals("712035338846994502"))
            g.removeRoleFromMember(mem, g.getRoleById("717297533294215258")).queue();
        else if (g.getId().equals("447357526997073930"))
            g.removeRoleFromMember(mem, g.getRoleById("761723664273899580")).queue();
        if (!json.has("mutes")) json.put("mutes", new JSONArray());
        JSONArray arr = json.getJSONArray("mutes");
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = (JSONObject) arr.get(i);
            if (obj.getString("user").equals(mem.getId())) indexes.add(i);
        }
        while (!indexes.isEmpty()) {
            System.out.println(indexes);
            arr.remove(indexes.remove(0));
            for (int i = 0; i < indexes.size(); i++) {
                indexes.set(i, indexes.get(i) - 1);
            }
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde entmutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        tco.sendMessage(builder.build()).queue();
        saveEmolgaJSON();
    }

    public static void tempBan(TextChannel tco, Member mod, Member mem, int time, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht bannen!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        g.ban(mem, 0, reason).queue();
        JSONObject json = getEmolgaJSON();
        if (!json.has("bans")) json.put("bans", new JSONArray());
        JSONArray arr = json.getJSONArray("bans");
        JSONObject obj = new JSONObject();
        obj.put("mod", mod.getId());
        obj.put("user", mem.getId());
        obj.put("reason", reason);
        obj.put("guild", g.getId());
        long delay = System.currentTimeMillis() + time * 1000;
        obj.put("delay", delay);
        arr.put(obj);
        banTimer(g, delay, time * 1000, mem.getId());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gebannt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        saveEmolgaJSON();
    }

    public static void banTimer(Guild g, long delay, int time, String mem) {
        JSONObject json = getEmolgaJSON();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!json.has("bans")) json.put("bans", new JSONArray());
                JSONArray arr = json.getJSONArray("bans");
                boolean success = false;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = (JSONObject) arr.get(i);
                    if (obj.getLong("delay") == delay) {
                        arr.remove(i);
                        success = true;
                    }
                }
                saveEmolgaJSON();
                if (success) {
                    g.unban(mem).queue();
                    System.out.println("Unbanned!");
                }
            }
        }, time);
    }

    public static void warn(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht muten!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        JSONObject json = getEmolgaJSON();
        if (!json.has("warns")) json.put("warns", new JSONArray());
        JSONArray arr = json.getJSONArray("warns");
        JSONObject obj = new JSONObject();
        obj.put("mod", mod.getId());
        obj.put("user", mem.getId());
        obj.put("reason", reason);
        arr.put(obj);
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde verwarnt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        saveEmolgaJSON();
    }

    public static String getMonIfPresent(HashMap<String, String> map, String pick) {
        for (String s : map.keySet()) {
            if (s.equals(pick) || s.equals(pick.substring(2))) {
                return pick;
            }
        }
        return "";
    }

    public static String getNumber(HashMap<String, String> map, String pick) {
        //System.out.println(map);
        for (String s : map.keySet()) {
            if (s.equals(pick) || s.equals(pick.substring(2))) {
                return map.get(s);
            }
        }
        return "";
    }

    public static String formatToTime(long l) {
        l /= 1000;
        int hours = (int) (l / 3600);
        int minutes = (int) ((l - hours * 3600) / 60);
        int seconds = (int) (l - hours * 3600 - minutes * 60);
        String str = "";
        if (hours > 0) str += getWithZeros(hours, 2) + ":";
        str += getWithZeros(minutes, 2) + ":";
        str += getWithZeros(seconds, 2);
        return str;
    }

    public static String getWithZeros(int i, int lenght) {
        StringBuilder str = new StringBuilder(i + "");
        while (str.length() < lenght) str.insert(0, "0");
        return str.toString();
    }

    public static <T> List<T> getXTimes(T object, int times) {
        ArrayList<T> list = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            list.add(object);
        }
        return list;
    }

    private static int compareColumns(List<Object> o1, List<Object> o2, int... columns) {
        for (int column : columns) {
            if (Integer.parseInt((String) o1.get(column)) != Integer.parseInt((String) o2.get(column))) {
                return Integer.compare(Integer.parseInt((String) o1.get(column)), Integer.parseInt((String) o2.get(column)));
            }
        }
        return 0;
    }

    public static void sortZBS(String sid, String tablename, JSONObject league) {
        List<List<Object>> formula = Google.get(sid, tablename + "!F13:M19", true, false);
        List<List<Object>> points = Google.get(sid, tablename + "!F13:M19", false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        JSONObject docnames = getEmolgaJSON().getJSONObject("docnames");
        points.sort((o1, o2) -> {
            int c = compareColumns(o1, o2, 1, 7, 5);
            if (c != 0) return c;
            String u1 = docnames.getString((String) o1.get(0));
            String u2 = docnames.getString((String) o2.get(0));
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ";" + u2)) return o.getString(u1 + ";" + u2).equals(u1) ? 1 : -1;
                if (o.has(u2 + ";" + u1)) return o.getString(u2 + ";" + u1).equals(u1) ? 1 : -1;
            }
            return 0;
        });
        Collections.reverse(points);
        //System.out.println(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 18; j++) {
            sendname.add(namap.get(j));
        }
        Google.updateRequest(sid, tablename + "!F13", sendname, false, false);
        System.out.println("Done!");
    }

    public static void sortWooloo(String sid, JSONObject league) {
        int i;
        List<List<Object>> formula = Google.get(sid, "Tabelle!D3:O39", true, false);
        List<List<Object>> p = Google.get(sid, "Tabelle!E3:P39", false, false);
        List<List<Object>> pf = Google.get(sid, "Tabelle!H3:H39", true, false);
        ArrayList<List<Object>> points = new ArrayList<>();
        for (i = 0; i < 40; i++) {
            if ((i + 4) % 4 == 0) {
                points.add(p.get(i));
            }
        }
        //System.out.println(points);
        List<List<Object>> orig = new ArrayList<>(points);

        points.sort((o1, o2) -> {
            if (Integer.parseInt((String) o1.get(3)) != Integer.parseInt((String) o2.get(3))) {
                return Integer.compare(Integer.parseInt((String) o1.get(3)), Integer.parseInt((String) o2.get(3)));
            }
            if (Integer.parseInt((String) o1.get(11)) != Integer.parseInt((String) o2.get(11))) {
                return Integer.compare(Integer.parseInt((String) o1.get(11)), Integer.parseInt((String) o2.get(11)));
            }
            if (Integer.parseInt((String) o1.get(9)) != Integer.parseInt((String) o2.get(9))) {
                return Integer.compare(Integer.parseInt((String) o1.get(9)), Integer.parseInt((String) o2.get(9)));
            }
            if (!league.has("results")) return -1;
            JSONObject results = league.getJSONObject("results");
            String n1 = (String) o1.get(0);
            String n2 = (String) o2.get(0);
            if (results.has(n1 + ":" + n2)) {
                return results.getString(n1 + ":" + n2).equals(n1) ? 1 : -1;
            }
            if (results.has(n2 + ":" + n1)) {
                return results.getString(n2 + ":" + n1).equals(n1) ? 1 : -1;
            }
            return -1;
        });
        Collections.reverse(points);
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        i = 0;
        Spreadsheet s = Google.getSheetData(sid, "Tabelle!E3:E39", false);
        Sheet sheet = s.getSheets().get(0);
        ArrayList<CellFormat> formats = new ArrayList<>();
        for (RowData rowDatum : sheet.getData().get(0).getRowData()) {
            if ((i + 4) % 4 != 0) {
                i++;
                continue;
            }
            formats.add(rowDatum.getValues().get(0).getEffectiveFormat());
            i++;
        }
        HashMap<Integer, CellFormat> formap = new HashMap<>();
        HashMap<Integer, String> stMap = new HashMap<>();
        i = 0;
        for (List<Object> objects : orig) {
            List<Object> list = formula.get(i * 4);
            int index = points.indexOf(objects);
            Object logo = list.remove(0);
            Object name = list.remove(0);
            list.subList(0, 5).clear();
            valmap.put(index, list);
            namap.put(index, Arrays.asList(logo, name));
            formap.put(index, formats.get(i));
            String teamname = (String) orig.get(i).get(0);
            stMap.put(index, "=SUMME(K" + (index * 4 + 3) + " * 3 + D" + (Arrays.asList(getEmolgaJSON().getJSONObject("drafts").getJSONObject("Wooloo Cup").getString("table").split(",")).indexOf(teamname) + 51) + ")");
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        List<List<Object>> sendname = new ArrayList<>();
        List<List<Object>> sendstyle = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            senddata.add(valmap.get(j));
            sendname.add(namap.get(j));
            sendstyle.add(Collections.singletonList(stMap.get(j)));
            for (int k = 0; k < 3; k++) {
                senddata.add(Collections.emptyList());
                sendname.add(Collections.emptyList());
                sendstyle.add(Collections.emptyList());
            }
        }
        Google.updateRequest(sid, "Tabelle!K3", senddata, false, false);
        Google.updateRequest(sid, "Tabelle!D3", sendname, false, false);
        Google.updateRequest(sid, "Tabelle!H3", sendstyle, false, false);
        for (int j = 0; j < 10; j++) {
            Request request = new Request();
            CellData cellData = new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setFontSize(formap.get(j).getTextFormat().getFontSize())));
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                    .setValues(Collections.singletonList(cellData))))
                    .setFields("userEnteredFormat.textFormat.fontSize").setRange(new GridRange().setSheetId(1102442612).setStartRowIndex(j * 4 + 2).setEndRowIndex(j * 4 + 3).setStartColumnIndex(4).setEndColumnIndex(5)));
            Google.batchUpdateRequest(sid, request, false);
        }
    }

    public static void loadAndPlay(final TextChannel channel, final String trackUrl, VoiceChannel vc) {
        GuildMusicManager musicManager = getGuildAudioPlayer(vc.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("`" + track.getInfo().title + "` wurde zur Warteschlange hinzugefügt!").queue();
                play(vc.getGuild(), musicManager, track, vc);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(vc.getGuild(), musicManager, firstTrack, vc);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Es wurde unter `" + trackUrl + "` nichts gefunden!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.getMessage()).queue();
                exception.printStackTrace();
            }
        });
    }

    public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, Member mem, TextChannel tc) {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            if (mem.getVoiceState().inVoiceChannel()) {
                audioManager.openAudioConnection(mem.getVoiceState().getChannel());
            } else {
                tc.sendMessage("Du musst dich in einem Voicechannel befinden!").queue();
            }
        }
        musicManager.scheduler.queue(track);
    }

    public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, VoiceChannel vc) {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(vc);
        }
        musicManager.scheduler.queue(track);
    }

    public static void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();
        channel.sendMessage("Skipped :)").queue();
    }

    public static synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public static String trim(String s, String pokemon) {
        return s
                .replaceAll(pokemon, stars(pokemon.length()))
                .replaceAll(pokemon.toUpperCase(), stars(pokemon.length()));
                /*.replaceAll("�", "ae")
                .replaceAll("�", "oe")
                .replaceAll("�", "ue");*/
    }

    public static String stars(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append("+");
        return s.toString();
    }

    public static JSONObject load(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) {//noinspection ResultOfMethodCallIgnored
                f.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write("{}");
                writer.close();
            }
            return new JSONObject(new JSONTokener(new FileReader(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getDataJSON() {
        return datajson;
    }

    public static JSONObject getMovesJSON() {
        return movejson;
    }

    public static JSONObject getWikiJSON() {
        return wikijson;
    }

    public static JSONObject getEmolgaJSON() {
        return emolgajson;
    }

    public static JSONObject getStatisticsJSON() {
        return statisticsjson;
    }

    public static JSONObject getLevelJSON() {
        return leveljson;
    }


    public static void saveStatisticsJSON() {
        save(statisticsjson, "statistics.json");
    }

    public static void saveEmolgaJSON() {
        save(emolgajson, "emolgadata.json");
    }

    public static void saveLevelJSON() {
        save(leveljson, "levels.json");
    }

    public static void save(JSONObject json, String filename) {
        try {
            /*if(!emolgajson.has("lastUpload")) emolgajson.put("lastUpload", new JSONObject());
            JSONObject lastUpload = emolgajson.getJSONObject("lastUpload");
            if(!lastUpload.has(filename)) lastUpload.put(filename, 0);
            if(System.currentTimeMillis() - lastUpload.getLong(filename) > 86400000) {
                lastUpload.put(filename, System.currentTimeMillis());
            }*/
            Files.copy(Paths.get(filename), Paths.get(filename + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(json.toString(4));
            writer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


    public static JSONObject getShinySpriteJSON() {
        return shinyspritejson;
    }

    public static JSONObject getSpriteJSON() {
        return spritejson;
    }

    private static void registerCommands() {
        new DataCommand();
        new MovesCommand();
        new CanlearnCommand();
        new SmogonCommand();
        new TierCommand();
        new AbilityCommand();
        new AttackCommand();
        new CombinationCommand();
        new GerCommand();
        new EnglCommand();
        new SpeedCommand();
        new AllLearnCommand();
        new BerryCommand();
        new NicknameCommand();
        new AslsortCommand();
        new CountuntilCommand();
        new DeleteuntilCommand();
        new LigaCommand();
        new ListallmembersCommand();
        new ListmembersCommand();
        new CreatedraftCommand();
        new DeldraftCommand();
        new DraftsetupCommand();
        new PickCommand();
        new SetupfromfileCommand();
        new SpielplanCommand();
        new StopdraftCommand();
        new UpdatebattleorderCommand();
        new UpdatedatafromfileCommand();
        new UpdateorderfromfileCommand();
        new UpdatepicksCommand();
        new DexquizCommand();
        new SolutionCommand();
        new TipCommand();
        new ByeCommand();
        new ChillCommand();
        new QueueClearCommand();
        new DcCommand();
        new DeepCommand();
        new GoinCommand();
        new MusicCommand();
        new NpCommand();
        new PlayCommand();
        new QlCommand();
        new QueueCommand();
        new SkipCommand();
        new AnalyseCommand();
        new ReplayCommand();
        new TabelleCommand();
        new AddCommand();
        new CheckCommand();
        new AddMonCommand();
        new DelMonCommand();
        new GcreateCommand();
        new WarnCommand();
        new WarnsCommand();
        new MuteCommand();
        new UnmuteCommand();
        new HuntCommand();
        new GiveMeAdminPermissionsCommand();
        new GetIdsCommand();
        new KickCommand();
        new TempMuteCommand();
        new TempBanCommand();
        new SetBirthdayCommand();
        new UpcomingBirthdaysCommand();
        new ClearCommand();
        new ResultCommand();
        new RoundCommand();
        new MuffinCommand();
    }

    public static void awaitNextDay() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (emolgajson.has("birthdays")) {
                    JSONObject birthdays = emolgajson.getJSONObject("birthdays");

                }
            }
        }, c.getTimeInMillis() - System.currentTimeMillis());
    }

    public static void init() {
        registerCommands();
        try {
            balls = Files.readAllLines(Paths.get("./balls.txt"));
            mons = Files.readAllLines(Paths.get("./tauschdoc.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        musicManagers = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        hazards.addAll(Arrays.asList("Tarnsteine", "Stachler", "Giftspitzen", "Klebenetz"));
        momentum.addAll(Arrays.asList("Voltwechsel", "Rollwende", "Teleport", "Stafette", "Abgangstirade", "Kehrtwende"));
        flinch.addAll(Arrays.asList(("Biss\n" +
                "Donnerzahn\n" +
                "Drachenstoß\n" +
                "Eisenschädel\n" +
                "Eiszahn\n" +
                "Eiszapfhagel\n" +
                "Elektropikser\n" +
                "Erstauner\n" +
                "Fegekick\n" +
                "Feuerzahn\n" +
                "Finsteraura\n" +
                "Fußkick\n" +
                "Herzstempel\n" +
                "Himmelsfeger\n" +
                "Hyperzahn\n" +
                "Kaskade\n" +
                "Knochenkeule\n" +
                "Kopfnuss\n" +
                "Luftschnitt\n" +
                "Mogelhieb\n" +
                "Nietenranke\n" +
                "Panzerfäuste\n" +
                "Quetschwalze\n" +
                "Schleuder\n" +
                "Schnarcher\n" +
                "Schwebesturz\n" +
                "Sondersensor\n" +
                "Stampfer\n" +
                "Steinhagel\n" +
                "Windhose\n" +
                "Zen-Kopfstoß").split("\n")));
        recovery.addAll(Arrays.asList(("Ableithieb\n" +
                "Absorber\n" +
                "Blubbsauger\n" +
                "Blutsauger\n" +
                "Diebeskuss\n" +
                "Dschungelheilung\n" +
                "Egelsamen\n" +
                "Erholung\n" +
                "Florakur\n" +
                "Genesung\n" +
                "Gigasauger\n" +
                "Heilbefehl\n" +
                "Heilopfer\n" +
                "Heilwoge\n" +
                "Holzgeweih\n" +
                "Kraftabsorber\n" +
                "Lebenstropfen\n" +
                "Leidteiler\n" +
                "Lunartanz\n" +
                "Läuterung\n" +
                "Megasauger\n" +
                "Milchgetränk\n" +
                "Mondschein\n" +
                "Morgengrauen\n" +
                "Parabolladung\n" +
                "Pollenknödel\n" +
                "Ruheort\n" +
                "Sandsammler\n" +
                "Synthese\n" +
                "Tagedieb\n" +
                "Traumfresser\n" +
                "Unheilsschwingen\n" +
                "Verwurzler\n" +
                "Verzehrer\n" +
                "Wasserring\n" +
                "Weichei\n" +
                "Wunschtraum").split("\n")));
        setup.addAll(Arrays.asList((
                "Agilität\n" +
                        "Akupressur\n" +
                        "Amnesie\n" +
                        "Aura-Rad\n" +
                        "Autotomie\n" +
                        "Backenstopfer\n" +
                        "Barriere\n" +
                        "Bauchtrommel\n" +
                        "Blockbefehl\n" +
                        "Coaching\n" +
                        "Doppelteam\n" +
                        "Drachentanz\n" +
                        "Einigler\n" +
                        "Einrollen\n" +
                        "Eisenabwehr\n" +
                        "Falterreigen\n" +
                        "Finalformation\n" +
                        "Fluch\n" +
                        "Gangwechsel\n" +
                        "Gedankengut\n" +
                        "Hausbruch\n" +
                        "Hilfsmechanik\n" +
                        "Horter\n" +
                        "Härtner\n" +
                        "Jauler\n" +
                        "Klauenwetzer\n" +
                        "Komprimator\n" +
                        "Kosmik-Kraft\n" +
                        "Kraftschub\n" +
                        "Ladevorgang\n" +
                        "Meditation\n" +
                        "Meteorstrahl\n" +
                        "Nitroladung\n" +
                        "Panzerschutz\n" +
                        "Protzer\n" +
                        "Ränkeschmied\n" +
                        "Rückenwind\n" +
                        "Schuppenschuss\n" +
                        "Schwerttanz\n" +
                        "Schädelwumme\n" +
                        "Schärfer\n" +
                        "Seelentanz\n" +
                        "Steinpolitur\n" +
                        "Säurepanzer\n" +
                        "Turbodreher\n" +
                        "Verzierung\n" +
                        "Wachstum\n" +
                        "Watteschild\n" +
                        "Sonnentag\n" +
                        "Regentanz\n" +
                        "Hagelsturm\n" +
                        "Sandsturm\n"
        ).split("\n")));
        serebiiex.put("Barschuft-B", "b");
        serebiiex.put("Riffex-H", "");
        serebiiex.put("Riffex-T", "-l");
        serebiiex.put("Wolwerock-Tag", "");
        serebiiex.put("Wolwerock-Nacht", "-m");
        serebiiex.put("Wolwerock-Dusk", "-d");
        serebiiex.put("Barschuft-R", "");
        serebiiex.put("Wulaosu-Unlicht", "");
        serebiiex.put("Wulaosu-Wasser", "-r");
        serebiiex.put("Basculin-B", "b");
        serebiiex.put("Toxtricity-H", "");
        serebiiex.put("Toxtricity-T", "-l");
        serebiiex.put("Lycanroc-Tag", "");
        serebiiex.put("Lycanroc-Nacht", "-m");
        serebiiex.put("Lycanroc-Dusk", "-d");
        serebiiex.put("Basculin-R", "");
        serebiiex.put("Urshifu-Unlicht", "");
        serebiiex.put("Urshifu-Wasser", "-r");
        serebiiex.put("Rotom-Wash", "-w");
        serebiiex.put("Rotom-Heat", "-h");
        serebiiex.put("Rotom-Fan", "-s");
        serebiiex.put("Rotom-Mow", "-m");
        serebiiex.put("Rotom-Frost", "-f");
        serebiiex.put("Demeteros-T", "-s");
        serebiiex.put("Voltolos-T", "-s");
        serebiiex.put("Boreos-I", "");
        serebiiex.put("Burmadame-Pflz", "");
        serebiiex.put("Burmadame-Sand", "-c");
        serebiiex.put("Burmadame-Lumpen", "-s");
        sdex.put("Boreos-T", "-therian");
        sdex.put("Demeteros-T", "-therian");
        sdex.put("Deoxys-Def", "-defense");
        sdex.put("Hoopa-U", "-unbound");
        sdex.put("Wulaosu-Wasser", "-rapidstrike");
        sdex.put("Demeteros-I", "");
        sdex.put("Rotom-Heat", "-heat");
        sdex.put("Rotom-Wash", "-wash");
        sdex.put("Rotom-Mow", "-mow");
        sdex.put("Rotom-Fan", "-fan");
        sdex.put("Rotom-Frost", "-frost");
        sdex.put("Wolwerock-Zw", "-dusk");
        sdex.put("Wolwerock-Tag", "");
        sdex.put("Wolwerock-Nacht", "-midnight");
        sdex.put("Boreos-I", "");
        sdex.put("Voltolos-T", "-therian");
        sdex.put("Voltolos-I", "");
        sdex.put("Zygarde-50%", "");
        sdex.put("Zygarde-10%", "-10");
        emolgachannel.put("518008523653775366", new ArrayList<>(Arrays.asList("728680506098712579", "736501675447025704")));
        emolgachannel.put("712035338846994502", new ArrayList<>(Arrays.asList("732545253344804914", "735076688144105493")));
        emolgachannel.put("709877545708945438", new ArrayList<>(Collections.singletonList("738893933462945832")));
        emolgachannel.put("677229415629062180", new ArrayList<>(Collections.singletonList("731455491527540777")));
        emolgachannel.put("694256540642705408", new ArrayList<>(Collections.singletonList("695157832072560651")));
        emolgachannel.put("747357029714231299", new ArrayList<>(Arrays.asList("752802115096674306", "762411109859852298")));
        loadJSONFiles();
        sdAnalyser.put("518008523653775366", (game, uid1, uid2, kills, deaths) -> {
            JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7");
            String leaguename = asl.keySet().stream().filter(s -> s.startsWith("PK") || s.equals("Coach")).filter(s -> Arrays.asList(asl.getJSONObject(s).getString("table").split(",")).contains(uid1)).collect(Collectors.joining(""));
            JSONObject league = asl.getJSONObject(leaguename);
            String table = league.getString("table");
            JSONArray teamsarray = asl.getJSONArray("teams");
            String t1 = teamsarray.getString(table.indexOf(uid1));
            String t2 = teamsarray.getString(table.indexOf(uid2));
            int pk = leaguename.equals("Coach") ? 0 : Integer.parseInt(leaguename.substring(2));
            String sid = league.getString("sid");
            int gameday = getGameDay(league, t1, t2);
            if (gameday == -1) {
                System.out.println("GAMEDAY -1");
                return;
            }
            ArrayList<String> userids = new ArrayList<>(Arrays.asList(uid1, uid2));
            ArrayList<String> teams = new ArrayList<>(Arrays.asList(t1, t2));

            for (int i = 0; i < 2; i++) {
                String uid = userids.get(i);
                String team = teams.get(i);
                ArrayList<String> picks = new ArrayList<>(Arrays.asList(league.getJSONObject("picks").getString(uid).split(",")));
                List<List<Object>> list = new ArrayList<>();
                for (String pick : picks) {
                    list.add(Arrays.asList(getMonIfPresent(kills, pick), getNumber(kills, pick), getNumber(deaths, pick)));
                }
                List<List<Object>> get;
                try {
                    get = Google.get(sid, team + "!O" + (pk + 4) + ":Q" + (pk + 4), false, false);
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                    return;
                }
                int win = Integer.parseInt((String) get.get(0).get(0));
                int loose = Integer.parseInt((String) get.get(0).get(1));
                if (game[i].isWinner()) {
                    win++;
                    /*if (!league.has("results"))
                        league.put("results", new JSONObject());
                    league.getJSONObject("results").put(uid1 + ":" + uid2, uid);*/
                } else loose++;
                try {
                    Google.updateRequest(sid, team + "!" + getAsXCoord(gameday * 3 + 8), list, false, false);
                    Google.updateRequest(sid, team + "!O" + (pk + 4), Collections.singletonList(Arrays.asList(win, "", loose)), false, false);
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                }

            }
            saveEmolgaJSON();
            sortASL();
        });
        sdAnalyser.put("709877545708945438", (game, uid1, uid2, kills, deaths) -> {
            Guild guild = EmolgaMain.jda.getGuildById("709877545708945438");
            Emote love = guild.getEmoteById("710842233712017478");
            Emote beep = guild.getEmoteById("745355018676469844");
            JSONObject json = getEmolgaJSON();
            JSONObject league = json.getJSONObject("drafts").getJSONObject("Wooloo Cup");
            if (!league.has("doc")) return;
            String sid = league.getJSONObject("doc").getString("sid");
            Message message = guild.getTextChannelById("749194448507764766").sendMessage(uid1 + " (" + love.getAsMention() + ") oder " + uid2 + " (" + beep.getAsMention() + ")?").complete();
            message.addReaction(love).queue();
            message.addReaction(beep).queue();
            if (!json.has("style")) json.put("style", new JSONArray());
            JSONArray style = json.getJSONArray("style");
            JSONObject obj = new JSONObject();
            obj.put("uid1", uid1);
            obj.put("uid2", uid2);
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
                        uid = uid1;
                    } else if (p1 < p2) {
                        uid = uid2;
                    } else {
                        uid = new Random().nextInt(2) == 0 ? uid1 : uid2;
                    }
                    Google.updateRequest(sid,
                            "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51),
                            Collections.singletonList(Collections.singletonList(
                                    Integer.parseInt((String) Google.get(sid, "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid1) + 51), false, false).get(0).get(0) + 1)
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
        });
        sdAnalyser.put("747357029714231299", (game, uid1, uid2, kills, deaths) -> {
            JSONObject league;
            JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
            int l;
            if (drafts.getJSONObject("ZBSL1").getString("table").contains(uid1)) l = 1;
            else l = 2;
            league = drafts.getJSONObject("ZBSL" + l);
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
        });
    }

    public static void loadJSONFiles() {
        emolgajson = load("./emolgadata.json");
        wikijson = load("./wikidata.json");
        datajson = load("./pkmndata.json");
        movejson = load("./moves.json");
        spritejson = load("./sprites.json");
        shinyspritejson = load("./shinysprites.json");
        huntjson = load("./hunt.json");
        statisticsjson = load("./statistics.json");
        ytjson = load("./yt.json");
        leveljson = load("./levels.json");
        tokens = load("./tokens.json");
        Google.REFRESHTOKEN = tokens.getJSONObject("google").getString("refreshtoken");
        Google.CLIENTID = tokens.getJSONObject("google").getString("clientid");
        Google.CLIENTSECRET = tokens.getJSONObject("google").getString("clientsecret");
        tradesid = tokens.getString("tradedoc");
    }

    public static List<Command> getWithCategory(CommandCategory category, Guild g, Member mem) {
        return commands.stream().filter(c -> c.category == category && c.allowsGuild(g) && (mem.hasPermission(Permission.ADMINISTRATOR) || !c.onlyAdmin)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static String getHelpDescripion(Guild g, Member mem) {
        return (allowsCategory(g, CommandCategory.Pokemon) ? "\uD83C\uDDF5 Pokemon\n" : "") +
                (allowsCategory(g, CommandCategory.Music) ? "\uD83C\uDDF2 Music\n" : "") +
                (allowsCategory(g, CommandCategory.Draft) ? "\uD83C\uDDE9 Draft\n" : "") +
                (allowsCategory(g, CommandCategory.Dexquiz) ? "\uD83C\uDDF6 DexQuiz\n" : "") +
                (allowsCategory(g, CommandCategory.BS) ? "\uD83C\uDDE7 Blazing Strikers\n" : "") +
                (allowsCategory(g, CommandCategory.Verschiedenes) ? "\uD83C\uDDFB Verschiedenes\n" : "") +
                (allowsCategory(g, CommandCategory.Admin) && mem.hasPermission(Permission.ADMINISTRATOR) ? "\uD83C\uDDE6 Admin\n" : "") +
                "\u25c0\ufe0f Zurück zur Übersicht";
    }

    public static void addReactions(Message m, Member mem) {
        Guild g = m.getGuild();
        if (allowsCategory(g, CommandCategory.Pokemon)) m.addReaction("\uD83C\uDDF5").queue();
        if (allowsCategory(g, CommandCategory.Music)) m.addReaction("\uD83C\uDDF2").queue();
        if (allowsCategory(g, CommandCategory.Draft)) m.addReaction("\uD83C\uDDE9").queue();
        if (allowsCategory(g, CommandCategory.Dexquiz)) m.addReaction("\uD83C\uDDF6").queue();
        if (allowsCategory(g, CommandCategory.BS)) m.addReaction("\uD83C\uDDE7").queue();
        if (allowsCategory(g, CommandCategory.Verschiedenes)) m.addReaction("\uD83C\uDDFB").queue();
        if (allowsCategory(g, CommandCategory.Admin)) m.addReaction("\uD83C\uDDE6").queue();
        m.addReaction("\u25c0\ufe0f").queue();
    }

    public static void help(TextChannel tco, Member mem) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Commands").setColor(Color.CYAN);
        builder.setDescription(getHelpDescripion(tco.getGuild(), mem));
        tco.sendMessage(builder.build()).queue(m -> {
            helps.add(m);
            addReactions(m, mem);
        });
    }

    public static void check(GuildMessageReceivedEvent e) {
        Member mem = e.getMember();
        String msg = e.getMessage().getContentDisplay();
        TextChannel tco = e.getChannel();
        String gid = e.getGuild().getId();
        for (Command command : commands) {
            if (command.category == CommandCategory.Flo && !mem.getId().equals("175910318608744448")) continue;
            if (command.checkPrefix(msg)) {
                if (!command.allowsGuild(tco.getGuild())) return;
                if (command.wip && !mem.getId().equals("175910318608744448")) {
                    tco.sendMessage("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!").queue();
                    return;
                }
                if (command.onlyAdmin && !mem.hasPermission(Permission.ADMINISTRATOR)) {
                    tco.sendMessage(NOPERM).queue();
                    return;
                }
                if (command.category != CommandCategory.Draft && command.category != CommandCategory.Flo && command.category != CommandCategory.Admin && !command.everywhere) {
                    if (command.overrideChannel.containsKey(gid)) {
                        List<String> l = command.overrideChannel.get(gid);
                        if (!l.contains(e.getChannel().getId())) {
                            e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                            return;
                        }
                    } else {
                        if (emolgachannel.containsKey(gid)) {
                            List<String> l = emolgachannel.get(gid);
                            if (!l.contains(e.getChannel().getId())) {
                                e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                                return;
                            }
                        }
                    }
                }
                try {
                    command.process(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\n" + command.getHelp(e.getGuild())).queue();
                }
            }
        }
    }

    public static String eachWordUpperCase(String s) {
        StringBuilder st = new StringBuilder();
        for (String str : s.split(" ")) {
            st.append(firstUpperCase(str)).append(" ");
        }
        return st.substring(0, st.length() - 1);
    }

    public static String firstUpperCase(String s) {
        return s.split("")[0].toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String getType(String str) {
        String s = str.toLowerCase();
        if (s.contains("--normal")) return "Normal";
        else if (s.contains("--kampf") || s.contains("--fighting")) return "Kampf";
        else if (s.contains("--flug") || s.contains("--flying")) return "Flug";
        else if (s.contains("--gift") || s.contains("--poison")) return "Gift";
        else if (s.contains("--boden") || s.contains("--ground")) return "Boden";
        else if (s.contains("--gestein") || s.contains("--rock")) return "Gestein";
        else if (s.contains("--käfer") || s.contains("--bug")) return "Käfer";
        else if (s.contains("--geist") || s.contains("--ghost")) return "Geist";
        else if (s.contains("--stahl") || s.contains("--steel")) return "Stahl";
        else if (s.contains("--feuer") || s.contains("--fire")) return "Feuer";
        else if (s.contains("--wasser") || s.contains("--water")) return "Wasser";
        else if (s.contains("--pflanze") || s.contains("--grass")) return "Pflanze";
        else if (s.contains("--elektro") || s.contains("--electric")) return "Elektro";
        else if (s.contains("--psycho") || s.contains("--psychic")) return "Psycho";
        else if (s.contains("--eis") || s.contains("--ice")) return "Eis";
        else if (s.contains("--drache") || s.contains("--dragon")) return "Drache";
        else if (s.contains("--unlicht") || s.contains("--dark")) return "Unlicht";
        else if (s.contains("--fee") || s.contains("--fairy")) return "Fee";
        return "";
    }

    public static String getClass(String str) {
        String s = str.toLowerCase();
        if (s.contains("--phys")) return "Physisch";
        else if (s.contains("--spez")) return "Spezial";
        else if (s.contains("--status")) return "Status";
        return "";
    }

    public static List<JSONObject> getAllForms(String monname) {
        JSONObject json = getDataJSON();
        return json.keySet().stream().filter(s -> s.startsWith(monname.toLowerCase()) && !s.endsWith("gmax") && (s.equalsIgnoreCase(monname) || json.getJSONObject(s).has("forme"))).sorted(Comparator.comparingInt(String::length)).map(json::getJSONObject).collect(Collectors.toList());
    }

    public static ArrayList<String> getAttacksFrom(String pokemon, String msg, String form) {
        return getAttacksFrom(pokemon, msg, form, 8);
    }

    public static ArrayList<String> getAttacksFrom(String pokemon, String msg, String form, int maxgen) {
        ArrayList<String> already = new ArrayList<>();
        String type = getType(msg);
        String dmgclass = getClass(msg);
        JSONObject movejson = getMovesJSON();
        boolean prio = !msg.toLowerCase().contains("--prio");
        boolean hazard = !msg.toLowerCase().contains("--hazards");
        boolean recover = !msg.toLowerCase().contains("--recovery");
        boolean setup = !msg.toLowerCase().contains("--setup");
        boolean momentum = !msg.toLowerCase().contains("--momentum");
        boolean flinch = !msg.toLowerCase().contains("--flinch");
        //boolean gen = !msg.contains("--gen");
        //boolean how = !msg.contains("--flinch");


        /*try {
            moves = new ArrayList<>(Arrays.asList(json.getJSONObject("pkmndata").getJSONObject(pokemon).getJSONObject("moves").getString(form).split(",")));
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>(Collections.singletonList("ERROR"));
        }*/
        //if (type.equals("") && dmgclass.equals("") && prio && hazard && recover && setup && momentum && flinch)
        //     return moves;
        JSONObject json = getWikiJSON();
        JSONObject atkdata = json.getJSONObject("atkdata");
        JSONObject data = getDataJSON();
        try {
            String str = pokemon.toLowerCase() + (form.equals("Normal") ? "" : form.toLowerCase());
            while (str != null) {
                JSONObject learnset = movejson.getJSONObject(str.replace("-", "")).getJSONObject("learnset");
                ArrayList<String> moves = new ArrayList<>(learnset.keySet());
                for (String move : moves) {
                    if (move.startsWith("Crypto")) continue;
                    if ((type.equals("") || atkdata.getJSONObject(move).getString("type").equals(type)) &&
                            (dmgclass.equals("") || atkdata.getJSONObject(move).getString("category").equals(dmgclass)) &&
                            (prio || atkdata.getJSONObject(move).getString("prio").contains("+")) &&
                            (hazard || Command.hazards.contains(move)) &&
                            (recover || Command.recovery.contains(move)) &&
                            (setup || Command.setup.contains(move)) &&
                            (momentum || Command.momentum.contains(move)) &&
                            (flinch || Command.flinch.contains(move)) &&
                            (containsGen(learnset, move, maxgen)) &&
                            !already.contains(move)) {
                        already.add(move);
                    }
                }
                JSONObject mon = data.getJSONObject(str);
                if (mon.has("prevo")) {
                    String s = mon.getString("prevo");
                    if (s.endsWith("-Alola") || s.endsWith("-Galar")) str = s.replaceAll("-", "").toLowerCase();
                    else str = s.toLowerCase();
                } else str = null;
            }


            /*if (!type.equals("") && dmgclass.equals("")) {
                for (String move : moves) {
                    if (atkdata.getJSONObject(move).getString("type").equals(type)) already.add(move);
                }
            } else if (type.equals("") && !dmgclass.equals("")) {
                for (String move : moves) {
                    if (atkdata.getJSONObject(move).getString("category").equals(dmgclass)) already.add(move);
                }
            } else {
                for (String move : moves) {
                    if (atkdata.getJSONObject(move).getString("type").equals(type) && atkdata.getJSONObject(move).getString("category").equals(dmgclass))
                        already.add(move);
                }
            }*/
        } catch (Exception ex) {
            sendToMe("Schau in die Konsole du kek!");
            ex.printStackTrace();
        }

        return already;
    }

    public static void sendToMe(String msg) {
        sendToUser("175910318608744448", msg);
    }

    public static void sendToUser(Member mem, String msg) {
        sendToUser(mem.getUser(), msg);
    }

    public static void sendToUser(User user, String msg) {
        user.openPrivateChannel().queue(pc -> pc.sendMessage(msg).queue());
    }

    public static void sendToUser(String id, String msg) {
        sendToUser(EmolgaMain.jda.retrieveUserById(id).complete(), msg);
    }

    private static boolean containsGen(JSONObject learnset, String move, int gen) {
        for (Object o : learnset.getJSONArray(move)) {
            String s = (String) o;
            for (int i = 1; i <= gen; i++) {
                if (s.startsWith(String.valueOf(i))) return true;
            }
        }
        return false;
    }

    public static String getAsXCoord(int i) {
        int x = 0;
        while (i - 26 > 0) {
            i -= 26;
            x++;
        }
        return (x > 0 ? (char) (x + 64) : "") + "" + (char) (i + 64);
    }

    public static String getGen5Sprite(String str) {
        String gitname;
        JSONObject data = getDataJSON();
        Optional<String> op = sdex.keySet().stream().filter(str::equalsIgnoreCase).findFirst();
        if (op.isPresent()) {
            String ex = op.get();
            String englname = getEnglName(ex.split("-")[0]);
            gitname = englname + sdex.get(str);
        } else {
            if (str.startsWith("M-")) {
                String sub = str.substring(2);
                if (str.endsWith("-X")) gitname = getEnglName(sub.substring(0, sub.length() - 2)) + "-megax";
                else if (str.endsWith("-Y")) gitname = getEnglName(sub.substring(0, sub.length() - 2)) + "-megay";
                else gitname = getEnglName(sub) + "-mega";
            } else if (str.startsWith("A-")) {
                gitname = getEnglName(str.substring(2)) + "-alola";
            } else if (str.startsWith("G-")) {
                gitname = getEnglName(str.substring(2)) + "-galar";
            } else if (str.startsWith("Amigento-")) {
                gitname = "Silvally-" + getEnglName(str.split("-")[1]);
            } else {
                gitname = getEnglName(str);
            }
        }
        return "=IMAGE(\"http://play.pokemonshowdown.com/sprites/gen5/" + gitname.toLowerCase().replace(" ", "") + ".png\"; 1)";
    }


    public static ArrayList<String> getTeamMates(String user) {
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7");
        String leaguename = asl.keySet().stream().filter(s -> s.startsWith("PK") || s.equals("Coach")).filter(s -> Arrays.asList(asl.getJSONObject(s).getString("table").split(",")).contains(user)).collect(Collectors.joining(""));
        JSONObject league = asl.getJSONObject(leaguename);
        int index = Arrays.asList(league.getString("table").split(",")).indexOf(user);
        ArrayList<String> list = new ArrayList<>();
        for (String s : asl.keySet()) {
            if (!s.equals("Coach") && !s.startsWith("PK")) continue;
            list.add(asl.getJSONObject(s).getString("table").split(",")[index]);
        }
        return list;
    }

    public static String getIconSprite(String str) {
        System.out.println("s = " + str);
        String gitname;
        JSONObject data = getDataJSON();
        if (str.toLowerCase().startsWith("a-")) {
            String gername = getGerName(str.substring(2));
            if (!gername.startsWith("pkmn;")) return "";
            gername = gername.substring(5);
            gitname = getWithZeros(data.getJSONObject(gername.toLowerCase()).getInt("num"), 3) + "-a";
        } else if (str.toLowerCase().startsWith("g-")) {
            String gername = getGerName(str.substring(2));
            if (!gername.startsWith("pkmn;")) return "";
            gername = gername.substring(5);
            gitname = getWithZeros(data.getJSONObject(gername.toLowerCase()).getInt("num"), 3) + "-g";
        } else if (serebiiex.keySet().stream().anyMatch(str::equalsIgnoreCase)) {
            String ex = serebiiex.keySet().stream().filter(str::equalsIgnoreCase).collect(Collectors.joining(""));
            String gername = getGerName(ex.split("-")[0]).substring(5);
            gitname = getWithZeros(data.getJSONObject(gername.toLowerCase()).getInt("num"), 3) + serebiiex.get(str);
        } else {
            String gername = getGerName(str);
            if (!gername.startsWith("pkmn;")) return "";
            gername = gername.substring(5);
            gitname = getWithZeros(data.getJSONObject(gername.toLowerCase()).getInt("num"), 3) + "";
        }
        String url = "https://www.serebii.net/pokedex-swsh/icon/" + gitname.toLowerCase() + ".png";
        System.out.println("url = " + url);
        /*BufferedImage img = null;
        try {
            img = ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int w = img.getWidth();
        int h = img.getHeight();
        // 52 * 21
        double dW = w * ((double) 35 / (double) h);
        int dWidth = (int) dW;
        int dHeight = 35;*/
        return "=IMAGE(\"" + url + "\"; 1)";
    }

    public static String getSugiLink(String str) {
        String gitname;
        JSONObject data = getDataJSON();
        if (str.startsWith("A-")) {
            gitname = getWithZeros(data.getJSONObject(str.substring(2).toLowerCase()).getInt("num"), 3) + "-a";
        } else if (str.startsWith("G-")) {
            gitname = getWithZeros(data.getJSONObject(str.substring(2).toLowerCase()).getInt("num"), 3) + "-g";
        } else if (serebiiex.containsKey(str)) {
            gitname = getWithZeros(data.getJSONObject(str.split("-")[0].toLowerCase()).getInt("num"), 3) + serebiiex.get(str);
        } else {
            gitname = getWithZeros(data.getJSONObject(str.toLowerCase()).getInt("num"), 3) + "";
        }
        return "https://www.serebii.net/swordshield/pokemon/" + gitname.toLowerCase() + ".png";
    }

    public static long calculateASLTimer() {
        return calculateTimer(12, 22, 120);
    }

    public static long calculateTimer(int from, int to, int delayinmins) {
        SimpleDateFormat f = new SimpleDateFormat("HH");
        long currmillis = System.currentTimeMillis();
        int after = Integer.parseInt(f.format(new Date(currmillis + delayinmins * 60000)));
        int now = Integer.parseInt(f.format(new Date(currmillis)));
        if ((now >= from && now < to) && (after < from || after >= to)) return ((to - from) * 60 + delayinmins) * 60000;
        else if (now >= to || now < from) {
            Calendar c = Calendar.getInstance();
            if (now >= to)
                c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, from + (delayinmins / 60));
            c.set(Calendar.MINUTE, (delayinmins % 60));
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis() - System.currentTimeMillis();
        } else return delayinmins * 60000;
    }

    /*SimpleDateFormat f = new SimpleDateFormat("HH");
        int after = Integer.parseInt(f.format(new Date(System.currentTimeMillis() + 7200000)));
        int now = Integer.parseInt(f.format(new Date(System.currentTimeMillis())));
        if ((now > 9 && now < 22) && (after < 10 || after > 21)) return 50400000;
        else if (now > 21 || now < 12) {
            Calendar c = Calendar.getInstance();
            if (now > 21)
                c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 14);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis() - System.currentTimeMillis();
        } else return 7200000;*/

    public static int getXPNeeded(int level) {
        return (int) (5 * (Math.pow(level, 2)) + 50 * level + 100);
    }

    public static int getLevelFromXP(int xp) {
        int remaining_xp = xp;
        int level = 0;
        while (remaining_xp >= getXPNeeded(level)) {
            remaining_xp -= getXPNeeded(level);
            level += 1;
        }
        return level;
    }

    public static void sortASL() {
        String sid = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getString("sid");
        List<List<Object>> formula = Google.get(sid, "Tabelle!B3:J14", true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle!C3:J14", false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        points.sort((o1, o2) -> compareColumns(o1, o2, 1, 7, 5));
        Collections.reverse(points);
        Spreadsheet s = Google.getSheetData(sid, "Tabelle!B3:B14", false);
        ArrayList<com.google.api.services.sheets.v4.model.Color> formats = new ArrayList<>();
        for (RowData rowDatum : s.getSheets().get(0).getData().get(0).getRowData()) {
            formats.add(rowDatum.getValues().get(0).getEffectiveFormat().getBackgroundColor());
        }
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        HashMap<Integer, com.google.api.services.sheets.v4.model.Color> formap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            int index = points.indexOf(objects);
            valmap.put(index, formula.get(i));
            System.out.println("index = " + index);
            System.out.println("formula.get(i) = " + formula.get(i));
            formap.put(index, formats.get(i));
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        //List<List<Object>> sendname = new ArrayList<>();
        ArrayList<RowData> rowData = new ArrayList<>();
        for (int j = 0; j < 12; j++) {
            senddata.add(valmap.get(j));
            rowData.add(new RowData().setValues(Collections.singletonList(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(formap.get(j))))));
        }
        System.out.println(senddata);
        Google.updateRequest(sid, "Tabelle!B3", senddata, false, false);
        Google.batchUpdateRequest(sid, new Request().setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(765460930).setStartRowIndex(2).setEndRowIndex(14).setStartColumnIndex(1).setEndColumnIndex(2))), false);
        System.out.println("Done!");
    }

    public static void sortBST() {
        JSONObject bst = getEmolgaJSON().getJSONObject("BST");
        String sid = bst.getString("sid");
        List<List<Object>> formula = Google.get(sid, "Tabelle!C3:L20", true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle!D3:M20", false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        JSONObject docnames = getEmolgaJSON().getJSONObject("docnames");
        points.sort((o1, o2) -> {
            int c = compareColumns(o1, o2, 2, 6, 9, 7);
            if (c != 0) return c;
            String u1 = docnames.getString((String) o1.get(0));
            String u2 = docnames.getString((String) o2.get(0));
            if (!bst.has("results")) return 0;
            JSONObject res = bst.getJSONObject("results");
            for (String s : res.keySet()) {
                JSONObject o = res.getJSONObject(s);
                if (o.has(u1 + ";" + u2)) return o.getString(u1 + ";" + u2).equals(u1) ? 1 : -1;
                if (o.has(u2 + ";" + u1)) return o.getString(u2 + ";" + u1).equals(u1) ? 1 : -1;
            }
            return 0;
        });
        Collections.reverse(points);
        //System.out.println(points);
        Spreadsheet s = Google.getSheetData(sid, "Tabelle!C3:C20", false);
        Sheet sheet = s.getSheets().get(0);
        ArrayList<com.google.api.services.sheets.v4.model.Color> formats = new ArrayList<>();
        for (RowData rowDatum : sheet.getData().get(0).getRowData()) {
            formats.add(rowDatum.getValues().get(0).getEffectiveFormat().getBackgroundColor());
        }
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        HashMap<Integer, com.google.api.services.sheets.v4.model.Color> formap = new HashMap<>();
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            List<Object> liste = formula.get(i);
            Object logo = liste.remove(0);
            Object name = liste.remove(0);
            liste.remove(0);
            int index = points.indexOf(objects);
            valmap.put(index, liste);
            formap.put(index, formats.get(i));
            namap.put(index, Arrays.asList(logo, name));
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        List<List<Object>> sendname = new ArrayList<>();
        ArrayList<RowData> rowData = new ArrayList<>();
        for (int j = 0; j < 18; j++) {
            senddata.add(valmap.get(j));
            sendname.add(namap.get(j));
            rowData.add(new RowData().setValues(Collections.singletonList(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(formap.get(j))))));
        }
        /*for (int j = 0; j < 18; j++) {
            System.out.println("sendname.get(j) = " + sendname.get(j));
            System.out.println("senddata.get(j) = " + senddata.get(j));
        }*/
        Google.updateRequest(sid, "Tabelle!F3", senddata, false, false);
        Google.updateRequest(sid, "Tabelle!C3", sendname, false, false);
        Request request = new Request();
        request.setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(1702581801).setStartRowIndex(2).setEndRowIndex(20).setStartColumnIndex(2).setEndColumnIndex(3)));
        Google.batchUpdateRequest(sid, request, false);
        System.out.println("Done!");
    }

    public static String divAndRound(int x, int y, boolean percent) {
        String str = String.valueOf(((double) x / (double) y) * (percent ? 100 : 1));
        //System.out.println(prozent + " str1 = " + str);
        if (str.length() <= 5) return str.equalsIgnoreCase("NaN") ? "0.0" : str;
        str = str.substring(0, str.indexOf(".") + 4);
        //System.out.println("str2 = " + str);
        double d = Double.parseDouble(str.substring(0, str.length() - 1));
        int i = Integer.parseInt(str.substring(str.length() - 1));
        //System.out.println("i = " + i);
        if (i >= 5) d += 0.01;
        return Double.toString(d).equalsIgnoreCase("NaN") ? "0.0" : Double.toString(d);
    }

    public static String getBSTGerName(String s) {
        String check = checkShortcuts(s);
        if (check != null) return check;
        System.out.println("BSTGerName s = " + s);
        String ret;
        if (s.equalsIgnoreCase("wulaosu") || s.equalsIgnoreCase("urshifu")) {
            return "ONLYWITHFORM";
        }
        if (serebiiex.keySet().stream().anyMatch(s::equalsIgnoreCase)) {
            String ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining(""));
            System.out.println("ex = " + ex);
            ret = getGerName(ex.split("-")[0]) + "-" + ex.split("-")[1];
            System.out.println("ret = " + ret);
            return ret;
        }
        if (s.toLowerCase().startsWith("a-")) {
            String gername = getGerName(s.substring(2));
            if (!gername.startsWith("pkmn;")) return "";
            ret = "pkmn;A-" + gername.substring(5);
            System.out.println("ret = " + ret);
            return ret;
        } else if (s.toLowerCase().startsWith("g-")) {
            String gername = getGerName(s.substring(2));
            if (!gername.startsWith("pkmn;")) return "";
            ret = "pkmn;G-" + gername.substring(5);
            System.out.println("ret = " + ret);
            return ret;
        }
        ret = getGerName(s);
        System.out.println("ret = " + ret);
        return ret;
    }

    public static String getDraftGerName(String s) {
        if (!getGerName(s).equals("")) return getGerName(s);
        String[] split = s.split("-");
        System.out.println("getDraftGerName s = " + s);
        System.out.println("getDraftGerName Arr = " + Arrays.toString(split));
        if (s.toLowerCase().startsWith("m-")) {
            String sub = s.substring(2);
            String mon;
            if (s.endsWith("-X")) mon = getGerName(sub.substring(0, sub.length() - 2)) + "-X";
            else if (s.endsWith("-Y")) mon = getGerName(sub.substring(0, sub.length() - 2)) + "-Y";
            else mon = getGerName(sub);
            if (!mon.startsWith("pkmn;")) return "";
            return "pkmn;M-" + mon.substring(5);
        } else if (s.toLowerCase().startsWith("a-")) {
            String mon = getGerName(s.substring(2));
            if (!mon.startsWith("pkmn;")) return "";
            return "pkmn;A-" + mon.substring(5);
        } else if (s.toLowerCase().startsWith("g-")) {
            String mon = getGerName(s.substring(2));
            if (!mon.startsWith("pkmn;")) return "";
            return "pkmn;G-" + mon.substring(5);
        }
        if (!getGerName(split[0]).equals("")) {
            String gername = getGerName(split[0]);
            String ret;
            if (gername.equals("pkmn;Amigento")) {
                if (split.length == 1) ret = "pkmn;Amigento-Normal";
                else {
                    String typ = getGerName(split[1]);
                    if (!typ.startsWith("type;")) ret = "";
                    else
                        ret = "pkmn;Amigento-" + typ.substring(5);
                }
            } else {
                ret = gername + "-" + split[1];
            }
            System.out.println("getDraftGerName ret = " + ret);
            return ret;
        }
        return "";
    }

    public static String getGerName(String s) {
        String check = checkShortcuts(s);
        if (check != null) return check;
        System.out.println("getGerName s = " + s);
        if (serebiiex.keySet().stream().anyMatch(s::equalsIgnoreCase)) {
            String ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining(""));
            System.out.println("ex = " + ex);
            String ret = getGerName(ex.split("-")[0]) + "-" + ex.split("-")[1];
            System.out.println("getGerName ret = " + ret);
            return ret;
        }
        if (s.equalsIgnoreCase("typ:null")) return "pkmn;Typ:Null";
        JSONObject json = getWikiJSON();
        String str = null;
        for (String translations : json.getJSONObject("translations").keySet()) {
            if (s.equalsIgnoreCase(translations)) {
                str = translations;
                break;
            }
        }
        if (str == null) return "";
        return json.getJSONObject("translations").getJSONObject(str).getString("type") + ";" + json.getJSONObject("translations").getJSONObject(str).getString("ger");
    }

    public static String getEnglName(String s) {
        String check = checkShortcuts(s);
        if (check != null) s = check.split(";")[1];
        JSONObject json = getWikiJSON();
        String str = null;
        for (String translations : json.getJSONObject("translations").keySet()) {
            if (s.equalsIgnoreCase(translations)) {
                str = translations;
                break;
            }
        }
        if (str == null) return "";
        return json.getJSONObject("translations").getJSONObject(str).getString("engl");
    }

    public static boolean canLearn(String pokemon, String form, String atk, String msg, int maxgen) {
        return getAttacksFrom(pokemon, msg, form, maxgen).contains(atk);
    }

    /*public static String getGerName(String s) {
        String check = checkShortcuts(s);
        if(check != null) return check;
        JSONObject json = getWikiJSON();
        if (!json.getJSONObject("translations").has(s)) {
            String str = eachWordUpperCase(s);
            if (!json.getJSONObject("translations").has(str)) return "";
            return json.getJSONObject("translations").getJSONObject(str).getString("type") + ";" + json.getJSONObject("translations").getJSONObject(str).getString("ger");
        } else
            return json.getJSONObject("translations").getJSONObject(s).getString("type") + ";" + json.getJSONObject("translations").getJSONObject(s).getString("ger");
    }

    public static String getEnglName(String s) {
        if (s.equalsIgnoreCase("STEIN")) return "Stonjourner";
        JSONObject json = getWikiJSON();
        if (!json.getJSONObject("translations").has(s)) {
            String str = eachWordUpperCase(s);
            if (!json.getJSONObject("translations").has(str)) return "";
            return json.getJSONObject("translations").getJSONObject(str).getString("engl");
        } else
            return json.getJSONObject("translations").getJSONObject(s).getString("engl");
    }*/

    public static String checkShortcuts(String s) {
        JSONObject json = getWikiJSON().getJSONObject("shortcuts");
        if (json.has(s.toLowerCase())) return json.getString(s.toLowerCase());
        return null;
    }

    public static String getMonName(String s, String gid) {
        System.out.println("s = " + s);
        if (gid.equals("709877545708945438")) {
            if (s.endsWith("-Alola")) {
                return "Alola-" + Command.getGerName(s.substring(0, s.length() - 6)).split(";")[1];
            } else if (s.endsWith("-Galar")) {
                return "Galar-" + Command.getGerName(s.substring(0, s.length() - 6)).split(";")[1];
            }
            switch (s) {
                case "Oricorio":
                    return "Choreogel-Feuer";
                case "Oricorio-Pa'u":
                    return "Choreogel-Psycho";
                case "Oricorio-Pom-Pom":
                    return "Choreogel-Elektro";
                case "Oricorio-Sensu":
                    return "Choreogel-Geist";
                case "Gourgeist":
                    return "Pumpdjinn";
                case "Gourgeist-Small":
                    return "Pumpdjinn-Small";
                case "Indeedee":
                    return "Servol-M";
                case "Indeedee-F":
                    return "Servol-W";
                case "Meowstic":
                    return "Psiaugon-M";
                case "Meowstic-F":
                    return "Psiaugon-W";
            }
        }
        if (s.equals("Wormadam")) return "Burmadame-Pflz";
        if (s.equals("Wormadam-Sandy")) return "Burmadame-Sand";
        if (s.equals("Wormadam-Trash")) return "Burmadame-Lumpen";
        if (s.contains("Minior")) return "Meteno";
        if (s.contains("Polteageist")) return "Mortipot";
        if (s.contains("Wormadam")) return "Burmadame";
        if (s.contains("Keldeo")) return "Keldeo";
        if (s.contains("Gastrodon")) return "Gastrodon";
        if (s.contains("Eiscue")) return "Kubuin";
        if (s.contains("Oricorio")) return "Choreogel";
        if (s.contains("Urshifu")) return "Wulaosu";
        if (s.contains("Gourgeist")) return "Pumpdjinn";
        if (s.contains("Pikachu")) return "Pikachu";
        if (s.contains("Indeedee")) return "Servol";
        if (s.contains("Meloetta")) return "Meloetta";
        if (s.contains("Alcremie")) return "Pokusan";
        if (s.contains("Mimikyu")) return "Mimigma";
        if (s.equals("Lycanroc")) return "Wolwerock-Tag";
        if (s.equals("Lycanroc-Midnight")) return "Wolwerock-Nacht";
        if (s.equals("Lycanroc-Dusk")) return "Wolwerock-Zw.";
        if (s.contains("Rotom")) return s;
        if (s.contains("Florges")) return "Florges";
        //System.out.println(s.contains("Silvally"));
        if (s.contains("Silvally")) {
            String[] split = s.split("-");
            if (split.length == 1 || s.equals("Silvally-*")) return "Amigento";
            else return "Amigento-" + getGerName(split[1]).split(";")[1];
        }
        //System.out.println("s = " + s);
        if (s.contains("Basculin")) return "Barschuft";
        if (s.contains("Sawsbuck")) return "Kronjuwild";
        if (s.equals("Kyurem-Black")) return "Kyurem-Black";
        if (s.equals("Kyurem-White")) return "Kyurem-White";
        if (s.equals("Meowstic")) return "Psiaugon-männlich";
        if (s.equals("Meowstic-F")) return "Psiaugon-weiblich";
        if (s.equalsIgnoreCase("Hoopa-Unbound")) return "Hoopa-U";
        if (s.equals("Zygarde")) return "Zygarde 50%";
        if (s.equals("Zygarde-10%")) return "Zygarde 10%";
        if (s.endsWith("-Mega")) {
            return "M-" + getGerName(s.substring(0, s.length() - 5)).split(";")[1];
        } else if (s.endsWith("-Alola")) {
            return "A-" + getGerName(s.substring(0, s.length() - 6)).split(";")[1];
        } else if (s.endsWith("-Galar")) {
            return "G-" + getGerName(s.substring(0, s.length() - 6)).split(";")[1];
        } else if (s.endsWith("-Therian")) {
            return getGerName(s.substring(0, s.length() - 8)).split(";")[1] + "-T";
        } else if (s.endsWith("-X")) {
            return "M-" + getGerName(s.split("-")[0]).split(";")[1] + "-X";
        } else if (s.endsWith("-Y")) {
            return "M-" + getGerName(s.split("-")[0]).split(";")[1] + "-Y";
        }
        if (s.equals("Tornadus")) return "Boreos-I";
        if (s.equals("Thundurus")) return "Voltolos-I";
        if (s.equals("Landorus")) return "Demeteros-I";
        //System.out.println(s);
        return getGerName(s).split(";")[1];
    }


    /*public static String getAbiGerName(String s) {
        JSONObject json = PnumaListener.getEmolgaJSON();
        String translations = json.getJSONObject("translations").getString("abi");
        return Arrays.stream(translations.split(";")).filter(str -> str.split(":")[0].toLowerCase().equalsIgnoreCase(s.toLowerCase()) || str.split(":")[1].toLowerCase().equalsIgnoreCase(s.toLowerCase())).collect(Collectors.joining("")).split(":")[0];
    }

    public static String getAbiEnglName(String s) {
        JSONObject json = PnumaListener.getEmolgaJSON();
        String translations = json.getJSONObject("translations").getString("abi");
        return Arrays.stream(translations.split(";")).filter(str -> str.split(":")[0].toLowerCase().equalsIgnoreCase(s.toLowerCase()) || str.split(":")[1].toLowerCase().equalsIgnoreCase(s.toLowerCase())).collect(Collectors.joining("")).split(":")[1];
    }*/

    public static boolean allowsCategory(Guild g, CommandCategory c) {
        String gid = g.getId();
        if(gid.equals("447357526997073930")) return true;
        if (c == CommandCategory.Music && !(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")))
            return false;
        return c != CommandCategory.BS || gid.equals("712035338846994502");
    }

    private boolean checkPrefix(String msg) {
        if (category == CommandCategory.Music) {
            return msg.toLowerCase().startsWith("e!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("e!" + s.toLowerCase() + " "))
                    || msg.equalsIgnoreCase("e!" + name) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("e!" + s));
        }
        return msg.toLowerCase().startsWith("!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("!" + s.toLowerCase() + " "))
                || msg.equalsIgnoreCase("!" + name.toLowerCase()) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("!" + s));
    }

    public boolean allowsGuild(Guild g) {
        if (!allowsCategory(g, category)) return false;
        return (allowedGuilds.size() == 0 || allowedGuilds.contains(g.getId()) || g.getId().equals("447357526997073930"));
    }

    public abstract void process(GuildMessageReceivedEvent e);

    public String getName() {
        return name;
    }

    public String getHelp() {
        return help;
    }

    public String getHelp(Guild g) {
        if (overrideHelp.containsKey(g.getId())) return overrideHelp.get(g.getId()) + (wip ? " (W.I.P.)" : "");
        return help + (wip ? " (W.I.P.)" : "");
    }
}
