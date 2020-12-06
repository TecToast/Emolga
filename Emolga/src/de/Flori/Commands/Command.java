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
import de.Flori.Commands.Flo.EmoteStealCommand;
import de.Flori.Commands.Flo.GetIdsCommand;
import de.Flori.Commands.Flo.GiveMeAdminPermissionsCommand;
import de.Flori.Commands.Music.*;
import de.Flori.Commands.Pokemon.*;
import de.Flori.Commands.Various.GcreateCommand;
import de.Flori.Commands.Various.NicknameCommand;
import de.Flori.Emolga.EmolgaMain;
import de.Flori.utils.Constants;
import de.Flori.utils.Google;
import de.Flori.utils.Music.GuildMusicManager;
import de.Flori.utils.ReplayAnalyser;
import de.Flori.utils.RequestBuilder;
import de.Flori.utils.Showdown.Player;
import de.Flori.utils.Showdown.SDPokemon;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Command {

    public static final String NOPERM = "Dafür hast du keine Berechtigung!";
    public static final File emolgadata = new File("./emolgadata.json");
    public static final ArrayList<Command> commands = new ArrayList<>();
    public static final ArrayList<String> hazards = new ArrayList<>();
    public static final ArrayList<String> recovery = new ArrayList<>();
    public static final ArrayList<String> setup = new ArrayList<>();
    public static final ArrayList<String> momentum = new ArrayList<>();
    public static final ArrayList<String> flinch = new ArrayList<>();
    public static final ArrayList<String> boom = new ArrayList<>();
    public static final ArrayList<String> trap = new ArrayList<>();
    public static final ArrayList<Guild> chill = new ArrayList<>();
    public static final ArrayList<Guild> deep = new ArrayList<>();
    public static final ArrayList<Guild> music = new ArrayList<>();
    public static final HashMap<CommandCategory, List<Command>> categorys = new HashMap<>();
    public static final HashSet<Message> helps = new HashSet<>();
    public static final HashMap<String, List<String>> emolgachannel = new HashMap<>();
    public static final HashMap<String, String> serebiiex = new HashMap<>();
    public static final HashMap<String, String> sdex = new HashMap<>();
    public static final HashMap<String, Long> latestExp = new HashMap<>();
    public static final HashMap<String, Double> expmultiplicator = new HashMap<>();
    public static final HashMap<String, ReplayAnalyser> sdAnalyser = new HashMap<>();
    public static final ArrayList<String> emotesteal = new ArrayList<>();
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
    public static JSONObject shinycountjson;
    public static JSONObject tokens;
    public static AudioPlayerManager playerManager;
    public static Map<Long, GuildMusicManager> musicManagers;
    public static ConcurrentLinkedQueue<byte[]> bytes = new ConcurrentLinkedQueue<>();
    public static boolean expEdited = false;
    public static Member as = null;
    public static boolean checkBST = false;
    protected static String tradesid;
    protected static List<String> balls;
    protected static List<String> mons;
    public final List<String> allowedGuilds;
    public final HashSet<String> aliases = new HashSet<>();
    public final HashMap<String, String> overrideHelp = new HashMap<>();
    public final HashMap<String, List<String>> overrideChannel = new HashMap<>();
    protected final String name;
    protected final String help;
    protected final CommandCategory category;
    public boolean onlyAdmin = false;
    protected boolean wip = false;
    protected boolean everywhere = false;
    protected Predicate<Member> isAllowed = m -> true;

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
        System.out.println("url = " + url);
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
        if (g.getId().equals(Constants.BSID))
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
                    if (g.getId().equals(Constants.BSID))
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
        if (g.getId().equals(Constants.BSID))
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
        if (g.getId().equals(Constants.BSID))
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
        JSONObject json = getEmolgaJSON().getJSONObject("warns");
        String gid = tco.getGuild().getId();
        if (!json.has(gid)) json.put(gid, new JSONArray());
        JSONArray arr = json.getJSONArray(tco.getGuild().getId());
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

    public static String getMonIfPresent(HashMap<String, String> map, String pick, int pk, int index) {
        if (pick.contains("Amigento") && map.containsKey("Amigento")) return "=C" + (pk * 19 + index + 18);
        for (String s : map.keySet()) {
            if (s.equals(pick) || s.equals(pick.substring(2))) {
                return "=C" + (pk * 19 + index + 18);
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
            int i1 = o1.get(column) instanceof Integer ? (int) o1.get(column) : Integer.parseInt((String) o1.get(column));
            int i2 = o2.get(column) instanceof Integer ? (int) o2.get(column) : Integer.parseInt((String) o2.get(column));
            if (i1 != i2) {
                return Integer.compare(i1, i2);
            }
        }
        return 0;
    }

    public static void updateShinyCounts() {
        StringBuilder b = new StringBuilder();
        JSONObject counter = shinycountjson.getJSONObject("counter");
        JSONObject names = shinycountjson.getJSONObject("names");
        for (String method : shinycountjson.getString("methodorder").split(",")) {
            JSONObject m = counter.getJSONObject(method);
            b.append(method).append(": ").append(EmolgaMain.jda.getGuildById("745934535748747364").getEmoteById(m.getString("emote")).getAsMention()).append("\n");
            for (String s : shinycountjson.getString("userorder").split(",")) {
                b.append(names.getString(s)).append(": ").append(m.optInt(s, 0)).append("\n");
            }
            b.append("\n");
        }
        EmolgaMain.jda.getTextChannelById("778380440078647296").editMessageById("778380596413464676", b.toString()).queue();
        save(shinycountjson, "shinycount.json");
    }

    //static Comparator<List<Object>> c = (Comparator) (o1, o2) -> 0;
    public static void sortTablesPerDay() {
        String sid = "18D3R5rX8kPpPU0BmdO79tWhWOlwLzoagmGlm3qVMAwg";
        RequestBuilder b = new RequestBuilder(sid);
        List<Request> l = new ArrayList<>();
        ArrayList<String> sheetranges = new ArrayList<>();
        ArrayList<String> pointsranges = new ArrayList<>();
        ArrayList<String> formularanges = new ArrayList<>();
        for (int gd = 1; gd <= 11; gd++) {
            sheetranges.add("Spieltag " + gd + "!B3:B14");
            formularanges.add("Spieltag " + gd + "!B3:J14");
            pointsranges.add("Spieltag " + gd + "!C3:J14");
        }
        Google.generateAccessToken();
        List<Sheet> sheets = Google.getSheetData(sid, false, sheetranges.toArray(new String[0])).getSheets();
        List<ValueRange> getformula = Google.batchGet(sid, formularanges, true, false);
        List<ValueRange> getpoints = Google.batchGet(sid, pointsranges, false, false);
        HashMap<Integer, Sheet> gdsheets = new HashMap<>();
        for (Sheet sheet : sheets) {
            gdsheets.put(Integer.valueOf(sheet.getProperties().getTitle().split(" ")[1]), sheet);
        }
        for (int gd = 1; gd <= 11; gd++) {
            List<List<Object>> formula = getformula.remove(0).getValues();
            List<List<Object>> points = getpoints.remove(0).getValues();
            List<List<Object>> orig = new ArrayList<>(points);
            points.sort((o1, o2) -> compareColumns(o1, o2, 1, 7, 5));
            Collections.reverse(points);
            Sheet sh = gdsheets.get(gd);
            ArrayList<com.google.api.services.sheets.v4.model.Color> formats = new ArrayList<>();
            for (RowData rowDatum : sh.getData().get(0).getRowData()) {
                formats.add(rowDatum.getValues().get(0).getEffectiveFormat().getBackgroundColor());
            }
            HashMap<Integer, List<Object>> valmap = new HashMap<>();
            HashMap<Integer, com.google.api.services.sheets.v4.model.Color> formap = new HashMap<>();
            int i = 0;
            for (List<Object> objects : orig) {
                int index = points.indexOf(objects);
                valmap.put(index, formula.get(i));
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
            b.addAll("Spieltag " + gd + "!B3", senddata);
            l.add(new Request().setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(sh.getProperties().getSheetId()).setStartRowIndex(2).setEndRowIndex(14).setStartColumnIndex(1).setEndColumnIndex(2))));
        }
        b.execute();
        try {
            Google.getSheetsService().spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest().setRequests(l)).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Spreadsheet s = Google.getSheetData(sid, false, "Tabelle!E3:E39");
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
        long guildId = guild.getIdLong();
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
        //new GoinCommand();
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
        new ShinyCommand();
        new CheckMonsCommand();
        new EmoteStealCommand();
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
        boom.addAll(Arrays.asList("Explosion,Finale,Nebelexplosion,Knallkopf".split(",")));
        trap.addAll(Arrays.asList(("Blitzgefängnis\n" +
                "Fangeisen\n" +
                "Feuerwirbel\n" +
                "Klammergriff\n" +
                "Lavasturm\n" +
                "Plage\n" +
                "Sandgrab\n" +
                "Schnapper\n" +
                "Whirlpool\n" +
                "Wickel").split("\n")));
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
        emolgachannel.put(Constants.ASLID, new ArrayList<>(Arrays.asList("728680506098712579", "736501675447025704")));
        emolgachannel.put(Constants.BSID, new ArrayList<>(Arrays.asList("732545253344804914", "735076688144105493")));
        emolgachannel.put("709877545708945438", new ArrayList<>(Collections.singletonList("738893933462945832")));
        emolgachannel.put("677229415629062180", new ArrayList<>(Collections.singletonList("731455491527540777")));
        emolgachannel.put("694256540642705408", new ArrayList<>(Collections.singletonList("695157832072560651")));
        emolgachannel.put("747357029714231299", new ArrayList<>(Arrays.asList("752802115096674306", "762411109859852298")));
        loadJSONFiles();
        sdAnalyser.put(Constants.ASLID, (game, uid1, uid2, kills, deaths, args) -> {
            JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7");
            String checkUid;
            if (uid1.equals("LSD")) checkUid = uid2;
            else checkUid = uid1;
            String leaguename = asl.keySet().stream().filter(s -> s.startsWith("PK") || s.equals("Coach")).filter(s -> Arrays.asList(asl.getJSONObject(s).getString("table").split(",")).contains(checkUid)).collect(Collectors.joining(""));
            System.out.println("leaguename = " + leaguename);
            JSONObject league = asl.getJSONObject(leaguename);
            List<String> table = Arrays.asList(league.getString("table").split(","));
            System.out.println("table = " + table);
            System.out.println("uid1 = " + uid1);
            System.out.println("uid2 = " + uid2);
            JSONArray teamsarray = asl.getJSONArray("teams");
            System.out.println("teamsarray = " + teamsarray.toString(4));
            String lsd = null;
            if (uid1.equals("LSD")) {
                uid1 = table.get(teamsarray.toList().indexOf("Lauras Sterndu"));
                lsd = EmolgaMain.jda.getGuildById(Constants.ASLID).retrieveMemberById(uid1).complete().getEffectiveName();
            } else if (uid2.equals("LSD")) {
                uid2 = table.get(teamsarray.toList().indexOf("Lauras Sterndu"));
                lsd = EmolgaMain.jda.getGuildById(Constants.ASLID).retrieveMemberById(uid2).complete().getEffectiveName();
            }
            int tin1 = table.indexOf(uid1);
            int tin2 = table.indexOf(uid2);
            System.out.println("tin1 = " + tin1);
            System.out.println("tin2 = " + tin2);
            String t1 = teamsarray.getString(tin1);
            String t2 = teamsarray.getString(tin2);
            int pk = leaguename.equals("Coach") ? 0 : Integer.parseInt(leaguename.substring(2));
            String sid = asl.getString("sid");
            int gameday = getGameDay(asl, t1, t2);
            if (gameday == -1) {
                System.out.println("GAMEDAY -1");
                return;
            }
            EmolgaMain.jda.getTextChannelById((String) args[1]).sendMessage("Spieltag " + gameday + "\nPreisklasse " + (pk == 0 ? "Coach" : pk) + "\n" + t1 + " vs " + t2 + "\n\n" + (lsd != null ? ((String) args[0]).replace("REPLACELSD", lsd) : args[0] + "")).queue();
            ArrayList<String> userids = new ArrayList<>(Arrays.asList(uid1, uid2));
            ArrayList<String> teams = new ArrayList<>(Arrays.asList(t1, t2));
            RequestBuilder b = new RequestBuilder(sid);
            for (int i = 0; i < 2; i++) {
                String uid = userids.get(i);
                String team = teams.get(i);
                ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                List<List<Object>> list = new ArrayList<>();
                for (int j = 0; j < picks.size(); j++) {
                    String pick = picks.get(j);
                    list.add(Arrays.asList(getMonIfPresent(kills, pick, pk, j), getNumber(kills, pick), getNumber(deaths, pick)));
                }
                try {
                    b.addAll(team + "!" + getAsXCoord(gameday * 3 + 8) + (pk * 19 + 18), list)
                            .addRow(team + "!" + getAsXCoord(gameday * 3 + 9) + (pk * 19 + 30), Arrays.asList(game[i].isWinner() ? 1 : 0, game[1 - i].isWinner() ? 1 : 0));
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                }
            }
            int aliveP1 = 0;
            int aliveP2 = 0;
            for (SDPokemon p : game[0].getMons()) {
                if (!p.isDead()) aliveP1++;
            }
            for (SDPokemon p : game[1].getMons()) {
                if (!p.isDead()) aliveP2++;
            }
            String str = null;
            int index = -1;
            List<String> battleorder = Arrays.asList(asl.getJSONObject("battleorder").getString(String.valueOf(gameday)).split("\\|"));
            for (String s : battleorder) {
                if (s.contains(t1)) {
                    str = s;
                    index = battleorder.indexOf(s);
                    break;
                }
            }
            if (str.split(":")[0].equals(t1)) {
                b.addSingle("Spielplan!" + getASLGameplanCoords(gameday, index, pk), aliveP1 + ":" + aliveP2);
            } else {
                b.addSingle("Spielplan!" + getASLGameplanCoords(gameday, index, pk), aliveP2 + ":" + aliveP1);
            }
            b.execute();
            saveEmolgaJSON();
            sortASL();
        });
        sdAnalyser.put("709877545708945438", (game, uid1, uid2, kills, deaths, args) -> {
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
                    woolooStyle(sid, message, uid1, uid2);
                }
            }, 86400000);
            saveEmolgaJSON();
            ArrayList<String> users = new ArrayList<>(Arrays.asList(uid1, uid2));
            int i = 0;
            RequestBuilder b = new RequestBuilder(sid);
            for (String uid : users) {
                int index = Arrays.asList(league.getString("table").split(",")).indexOf(uid);
                String range;
                if (index < 4)
                    range = "Teamübersicht!" + getAsXCoord(index * 6 + 4) + "7:" + getAsXCoord(index * 6 + 5) + "18";
                else if (index < 8)
                    range = "Teamübersicht!" + getAsXCoord((index - 4) * 6 + 4) + "24:" + getAsXCoord((index - 4) * 6 + 5) + "35";
                else
                    range = "Teamübersicht!" + getAsXCoord((index - 8) * 6 + 16) + "41:" + getAsXCoord((index - 8) * 6 + 17) + "52";
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
                b.addAll(urange, list);
                b.addRow("Teamübersicht!C" + (index + 40), Arrays.asList(win, loose), true, false);
                i++;
            }
            b.execute();
            sortWooloo(sid, league);
        });
        sdAnalyser.put("747357029714231299", (game, uid1, uid2, kills, deaths, args) -> {
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
            RequestBuilder b = new RequestBuilder(sid);
            for (String uid : users) {
                int index = Arrays.asList(league.getString("table").split(",")).indexOf(uid);
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
                    b.addAll("Liga " + l + "!B" + (index * 11 + 200), list);
                    b.addRow("Liga " + l + "!I" + (index + 3), Arrays.asList(win, getvic.get(0).get(1), loose), false, false);
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                }
                i++;
            }
            generateResult(b, game, league, gameday, uid1, "Spielplan Liga " + l, "ZBS");
            b.execute();
            sortZBS(sid, "Liga " + l, league);
            saveEmolgaJSON();
        });
    }

    public static void generateResult(RequestBuilder b, Player[] game, JSONObject league, int gameday, String uid1, String sheet, String leaguename) {
        int aliveP1 = 0;
        int aliveP2 = 0;
        for (SDPokemon p : game[0].getMons()) {
            if (!p.isDead()) aliveP1++;
        }
        for (SDPokemon p : game[1].getMons()) {
            if (!p.isDead()) aliveP2++;
        }
        String str = null;
        int index = -1;
        List<String> battleorder = Arrays.asList(league.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";"));
        for (String s : battleorder) {
            if (s.contains(uid1)) {
                str = s;
                index = battleorder.indexOf(s);
                break;
            }
        }
        String coords = null;
        switch (leaguename) {
            case "ZBS":
                coords = getZBSGameplanCoords(gameday, index);
                break;
            case "Wooloo":
                coords = getWoolooGameplanCoords(gameday, index);
                break;
        }
        if (str.split(":")[0].equals(uid1)) {
            b.addSingle(sheet + "!" + coords, aliveP1 + ":" + aliveP2);
        } else {
            b.addSingle(sheet + "!" + coords, aliveP2 + ":" + aliveP1);
        }
    }

    public static List<String> getMonList() {
        return getDataJSON().keySet().stream().filter(s -> !s.endsWith("gmax")).collect(Collectors.toList());
    }

    public static void woolooStyle(String sid, Message message, String uid1, String uid2) {
        Guild guild = EmolgaMain.jda.getGuildById("709877545708945438");
        Emote love = guild.getEmoteById("710842233712017478");
        Emote beep = guild.getEmoteById("745355018676469844");
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
        new RequestBuilder(sid).addSingle("Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51),
                Integer.parseInt((String) Google.get(sid, "Tabelle!D" + (Arrays.asList(league.getString("table").split(",")).indexOf(uid) + 51), false, false).get(0).get(0)) + 1)
                .execute();
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
        sortWooloo(sid, league);
    }

    private static String getZBSGameplanCoords(int gameday, int index) {
        if (gameday < 4) return "C" + (gameday * 5 + index - 2);
        if (gameday < 7) return "F" + ((gameday - 3) * 5 + index - 2);
        return "I" + (index + 3);
    }

    private static String getWoolooGameplanCoords(int gameday, int index) {
        if (gameday < 4) return "C" + (gameday * 6 + index - 2);
        if (gameday < 7) return "F" + ((gameday - 3) * 6 + index - 2);
        return "I" + ((gameday - 6) * 6 + index - 2);
    }

    private static String getASLGameplanCoords(int gameday, int index, int pk) {
        return getAsXCoord(gameday * 5 - 3) + (index * 6 + pk + 3);
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
        shinycountjson = load("./shinycount.json");
        tokens = load("./tokens.json");
        JSONObject google = tokens.getJSONObject("google");
        Google.setCredentials(google.getString("refreshtoken"), google.getString("clientid"), google.getString("clientsecret"));
        tradesid = tokens.getString("tradedoc");
        Google.generateAccessToken();
    }

    public static List<Command> getWithCategory(CommandCategory category, Guild g, Member mem) {
        return commands.stream().filter(c -> c.category == category && category.allowsGuild(g) && (mem.hasPermission(Permission.ADMINISTRATOR) || !c.onlyAdmin)).collect(Collectors.toCollection(ArrayList::new));
    }


    public static String getHelpDescripion(Guild g, Member mem) {
        StringBuilder s = new StringBuilder();
        for (CommandCategory cat : CommandCategory.getOrder()) {
            if (cat.allowsGuild(g) && cat.allowsMember(mem) && getWithCategory(cat, g, mem).size() > 0)
                s.append(cat.emoji).append(" ").append(cat.name).append("\n");
        }
        s.append("\u25c0\ufe0f Zurück zur Übersicht");
        return s.toString();
    }

    public static void addReactions(Message m, Member mem) {
        Guild g = m.getGuild();
        for (CommandCategory cat : CommandCategory.getOrder()) {
            if (cat.allowsGuild(g) && cat.allowsMember(mem)) m.addReaction(cat.emoji).queue();
        }
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
            if (command.category == CommandCategory.Flo && !mem.getId().equals(Constants.FLOID)) continue;
            if (command.checkPrefix(msg)) {
                if (command.category == CommandCategory.Music) {
                    tco.sendMessage("Die Musikfunktionen wurden aufgrund einer Fehlfunktion komplett deaktiviert!").queue();
                    return;
                }
                if (!command.category.allowsGuild(tco.getGuild())) return;
                if (!command.allowedGuilds.isEmpty() && !command.allowedGuilds.contains(gid) && !gid.equals("447357526997073930"))
                    return;
                if (command.wip && !mem.getId().equals(Constants.FLOID)) {
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
                    tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo/TecToast.\n" + command.getHelp(e.getGuild()) + (mem.getId().equals(Constants.FLOID) ? "\nJa, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
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
        JSONObject mon = json.getJSONObject(monname.toLowerCase());
        if (!mon.has("formeOrder")) return Collections.singletonList(mon);
        return mon.getJSONArray("formeOrder").toList().stream().map(o -> json.getJSONObject(((String) o).toLowerCase().replace("-", "").replace("%", ""))).collect(Collectors.toList());
        //return json.keySet().stream().filter(s -> s.startsWith(monname.toLowerCase()) && !s.endsWith("gmax") && (s.equalsIgnoreCase(monname) || json.getJSONObject(s).has("forme"))).sorted(Comparator.comparingInt(String::length)).map(json::getJSONObject).collect(Collectors.toList());
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
        boolean boom = !msg.toLowerCase().contains("--boom");
        boolean trap = !msg.toLowerCase().contains("--trap");
        //boolean gen = !msg.contains("--gen");


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
                            (boom || Command.boom.contains(move)) &&
                            (trap || Command.trap.contains(move)) &&
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
        sendToUser(Constants.FLOID, msg);
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

    public static List<List<Object>> getBlitzTable(boolean asIds) {
        List<List<Object>> list = new ArrayList<>();
        JSONObject json = getEmolgaJSON().getJSONObject("BlitzTurnier");
        ArrayList<String> players = new ArrayList<>(Arrays.asList(json.getString("players").split(",")));
        HashMap<String, String> names = new HashMap<>();
        if (!asIds)
            EmolgaMain.jda.getGuildById(Constants.BSID).retrieveMembersByIds(players.toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
        JSONObject playerstats = json.getJSONObject("playerstats");
        for (String player : players) {
            List<Object> l = new ArrayList<>();
            l.add(asIds ? player : names.get(player));
            JSONObject stats = playerstats.has(player) ? playerstats.getJSONObject(player) : new JSONObject();
            int wins = stats.optInt("wins", 0);
            int looses = stats.optInt("looses", 0);
            int bo3wins = stats.optInt("bo3wins", 0);
            int bo3looses = stats.optInt("bo3looses", 0);
            int kills = stats.optInt("kills", 0);
            int deaths = stats.optInt("deaths", 0);
            l.add(wins + looses);
            l.add(wins);
            l.add(looses);
            l.add(bo3wins);
            l.add(bo3looses);
            l.add(bo3wins - bo3looses);
            l.add(kills);
            l.add(deaths);
            l.add(kills - deaths);
            list.add(l);
        }
        list.sort((o1, o2) -> compareColumns(o1, o2, 2, 6, 9));
        Collections.reverse(list);
        return list;
    }

    public static void updateTable(JSONObject json, TextChannel tc) {
        new Thread(() -> {
            StringBuilder str = new StringBuilder("```");
            List<List<Object>> list = getBlitzTable(false);
            ArrayList<String> send = new ArrayList<>();
            ArrayList<Integer> name = new ArrayList<>();
            ArrayList<Integer> vic = new ArrayList<>();
            ArrayList<Integer> bo3dif = new ArrayList<>();
            //ArrayList<Integer> deaths = new ArrayList<>();
            ArrayList<Integer> dif = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                List<Object> l = list.get(i);
                String s = (i + 1) + ". " + (i < 9 ? " " : "") + l.get(0);
                name.add(s.length());
                vic.add((String.valueOf(l.get(2))).length());
                bo3dif.add((String.valueOf(l.get(4))).length());
                //deaths.add(((String) l.get(5)).length());
                dif.add((String.valueOf(l.get(6))).length());
            }
            int maxname = Collections.max(name);
            //System.out.println("maxname = " + maxname);
            int maxvic = Collections.max(vic);
            int maxkills = Collections.max(bo3dif);
            //int maxdeaths = Collections.max(deaths);
            int maxdif = Collections.max(dif);
            int maxbo3dif = Collections.max(bo3dif);
            String seperator = "   ";
            for (int i = 0; i < list.size(); i++) {
                List<Object> l = list.get(i);
                str.append(expandTo((i + 1) + ". " + (i < 9 ? " " : "") + l.get(0), maxname)).append(seperator).append(expandTo(String.valueOf(l.get(2)), maxvic)).append(" S.").append(seperator).append(expandTo(String.valueOf(l.get(6)), maxbo3dif)).append(" BO3 Dif.").append(seperator).append(expandTo(String.valueOf(l.get(9)), maxdif)).append(" Dif.\n");
                //System.out.println();
            }
            tc.editMessageById(tc.getLatestMessageId(), str.append("```").toString()).queue();
        }).start();
    }

    public static String expandTo(String str, int i) {
        StringBuilder strBuilder = new StringBuilder(str);
        while (strBuilder.length() < i) strBuilder.append(" ");
        //System.out.println("'" + strBuilder.toString() + "'");
        return strBuilder.toString();
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

    public static void sortGamedayList() throws IOException {
        String sid = "18D3R5rX8kPpPU0BmdO79tWhWOlwLzoagmGlm3qVMAwg";
        RequestBuilder b = new RequestBuilder(sid);
        List<Request> l = new ArrayList<>();
        ArrayList<String> sheetranges = new ArrayList<>();
        ArrayList<String> pointsranges = new ArrayList<>();
        ArrayList<String> formularanges = new ArrayList<>();
        for (int gd = 1; gd <= 11; gd++) {
            sheetranges.add("Spieltag " + gd + "!B3:B14");
            formularanges.add("Spieltag " + gd + "!B3:J14");
            pointsranges.add("Spieltag " + gd + "!C3:J14");
        }
        Google.generateAccessToken();
        List<Sheet> sheets = Google.getSheetData(sid, false, sheetranges.toArray(new String[0])).getSheets();
        List<ValueRange> getformula = Google.batchGet(sid, formularanges, true, false);
        List<ValueRange> getpoints = Google.batchGet(sid, pointsranges, false, false);
        HashMap<Integer, Sheet> gdsheets = new HashMap<>();
        for (Sheet sheet : sheets) {
            gdsheets.put(Integer.valueOf(sheet.getProperties().getTitle().split(" ")[1]), sheet);
        }
        for (int gd = 1; gd <= 11; gd++) {
            List<List<Object>> formula = getformula.remove(0).getValues();
            List<List<Object>> points = getpoints.remove(0).getValues();
            List<List<Object>> orig = new ArrayList<>(points);
            points.sort((o1, o2) -> compareColumns(o1, o2, 1, 7, 5));
            Collections.reverse(points);
            Sheet sh = gdsheets.get(gd);
            ArrayList<com.google.api.services.sheets.v4.model.Color> formats = new ArrayList<>();
            for (RowData rowDatum : sh.getData().get(0).getRowData()) {
                formats.add(rowDatum.getValues().get(0).getEffectiveFormat().getBackgroundColor());
            }
            HashMap<Integer, List<Object>> valmap = new HashMap<>();
            HashMap<Integer, com.google.api.services.sheets.v4.model.Color> formap = new HashMap<>();
            int i = 0;
            for (List<Object> objects : orig) {
                int index = points.indexOf(objects);
                valmap.put(index, formula.get(i));
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
            b.addAll("Spieltag " + gd + "!B3", senddata);
            l.add(new Request().setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(sh.getProperties().getSheetId()).setStartRowIndex(2).setEndRowIndex(14).setStartColumnIndex(1).setEndColumnIndex(2))));
        }
        b.execute();
        Google.getSheetsService().spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest().setRequests(l)).execute();
    }

    public static void sortASL() {
        String sid = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getString("sid");
        List<List<Object>> formula = Google.get(sid, "Tabelle!B3:J14", true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle!C3:J14", false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        points.sort((o1, o2) -> compareColumns(o1, o2, 1, 7, 5));
        Collections.reverse(points);
        Spreadsheet s = Google.getSheetData(sid, false, "Tabelle!B3:B14");
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
        Google.updateRequest(sid, "Tabelle!B3", senddata, false, false);
        Google.batchUpdateRequest(sid, new Request().setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(765460930).setStartRowIndex(2).setEndRowIndex(14).setStartColumnIndex(1).setEndColumnIndex(2))), false);
        System.out.println("Done!");
        sortTablesPerDay();
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
        Spreadsheet s = Google.getSheetData(sid, false, "Tabelle!C3:C20");
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
            String[] ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining("")).split("-");
            return getGerName(ex[0]) + "-" + ex[1];
        }
        if (s.equalsIgnoreCase("typ:null")) return "pkmn;Typ:Null";
        JSONObject t = getWikiJSON().getJSONObject("translations");
        return t.keySet().stream().filter(s::equalsIgnoreCase).findFirst().map(value -> t.getJSONObject(value).getString("type") + ";" + t.getJSONObject(value).getString("ger")).orElse("");
    }

    public static String getEnglName(String s) {
        String check = checkShortcuts(s);
        if (check != null) s = check.split(";")[1];
        JSONObject json = getWikiJSON().getJSONObject("translations");
        return json.keySet().stream().filter(s::equalsIgnoreCase).findFirst().map(value -> json.getJSONObject(value).getString("engl")).orElse("");
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
        return getWikiJSON().getJSONObject("shortcuts").optString(s.toLowerCase(), null);
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
        if (s.equals("Greninja-Ash")) return "Ash-Quajutsu";
        if (s.contains("Furfrou")) return "Coiffwaff";
        if (s.equals("Wormadam")) return "Burmadame-Pflz";
        if (s.equals("Wormadam-Sandy")) return "Burmadame-Sand";
        if (s.equals("Wormadam-Trash")) return "Burmadame-Lumpen";
        if (s.equals("Deoxys-Defense")) return "Deoxys-Def";
        if (s.contains("Minior")) return "Meteno";
        if (s.contains("Polteageist")) return "Mortipot";
        if (s.contains("Wormadam")) return "Burmadame";
        if (s.contains("Keldeo")) return "Keldeo";
        if (s.contains("Gastrodon")) return "Gastrodon";
        if (s.contains("Eiscue")) return "Kubuin";
        if (s.contains("Oricorio")) return "Choreogel";
        if (gid.equals(Constants.ASLID) && s.contains("Urshifu")) return "Wulaosu-Wasser";
        if (s.contains("Urshifu")) return "Wulaosu";
        if (s.contains("Gourgeist")) return "Pumpdjinn";
        if (s.contains("Pikachu")) return "Pikachu";
        if (s.contains("Indeedee")) return "Servol";
        if (s.contains("Meloetta")) return "Meloetta";
        if (s.contains("Alcremie")) return "Pokusan";
        if (s.contains("Mimikyu")) return "Mimigma";
        if (s.equals("Lycanroc")) return "Wolwerock-Tag";
        if (s.equals("Lycanroc-Midnight")) return "Wolwerock-Nacht";
        if (s.equals("Lycanroc-Dusk")) return "Wolwerock-Zw";
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
        if (s.equals("Zygarde")) return "Zygarde-50%";
        if (s.equals("Zygarde-10%")) return "Zygarde-10%";
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


    private boolean checkPrefix(String msg) {
        if (category == CommandCategory.Music) {
            return msg.toLowerCase().startsWith("e!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("e!" + s.toLowerCase() + " "))
                    || msg.equalsIgnoreCase("e!" + name) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("e!" + s));
        }
        return msg.toLowerCase().startsWith("!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("!" + s.toLowerCase() + " "))
                || msg.equalsIgnoreCase("!" + name.toLowerCase()) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("!" + s));
    }

    public abstract void process(GuildMessageReceivedEvent e);

    public String getName() {
        return name;
    }

    public String getHelp() {
        return help;
    }

    public String getHelp(Guild g) {
        return overrideHelp.getOrDefault(g.getId(), help) + (wip ? " (W.I.P.)" : "");
    }
}
