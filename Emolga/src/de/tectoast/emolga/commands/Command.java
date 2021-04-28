package de.tectoast.emolga.commands;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.youtube.model.SearchResult;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.admin.*;
import de.tectoast.emolga.commands.bs.*;
import de.tectoast.emolga.commands.dexquiz.DexquizCommand;
import de.tectoast.emolga.commands.dexquiz.SolutionCommand;
import de.tectoast.emolga.commands.dexquiz.TipCommand;
import de.tectoast.emolga.commands.draft.*;
import de.tectoast.emolga.commands.flo.EmolgaDiktaturCommand;
import de.tectoast.emolga.commands.flo.EmoteStealCommand;
import de.tectoast.emolga.commands.flo.GetIdsCommand;
import de.tectoast.emolga.commands.flo.GiveMeAdminPermissionsCommand;
import de.tectoast.emolga.commands.moderator.*;
import de.tectoast.emolga.commands.music.*;
import de.tectoast.emolga.commands.pokemon.*;
import de.tectoast.emolga.commands.showdown.AnalyseCommand;
import de.tectoast.emolga.commands.showdown.ReplayCommand;
import de.tectoast.emolga.commands.showdown.SearchReplaysCommand;
import de.tectoast.emolga.commands.showdown.SpoilerTagsCommand;
import de.tectoast.emolga.commands.various.*;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.*;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.showdown.Analysis;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.SDPokemon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.tectoast.emolga.bot.EmolgaMain.jda;

public abstract class Command {

    public static final String NOPERM = "Dafür hast du keine Berechtigung!";
    public static final File emolgadata = new File("./emolgadata.json");
    public static final ArrayList<Command> commands = new ArrayList<>();
    public static final ArrayList<Guild> chill = new ArrayList<>();
    public static final ArrayList<Guild> deep = new ArrayList<>();
    public static final ArrayList<Guild> music = new ArrayList<>();
    public static final HashSet<Message> helps = new HashSet<>();
    public static final HashMap<Long, List<Long>> emolgachannel = new HashMap<>();
    public static final HashMap<String, String> serebiiex = new HashMap<>();
    public static final HashMap<String, String> sdex = new HashMap<>();
    public static final HashMap<Long, Long> latestExp = new HashMap<>();
    public static final HashMap<Long, Double> expmultiplicator = new HashMap<>();
    public static final HashMap<Long, ReplayAnalyser> sdAnalyser = new HashMap<>();
    public static final ArrayList<Long> emoteSteal = new ArrayList<>();
    public static final HashMap<Long, Long> mutedRoles = new HashMap<>();
    public static final HashMap<Long, Long> moderatorRoles = new HashMap<>();
    public static final HashMap<Long, Long> replayAnalysis = new HashMap<>();
    public static final ArrayList<Long> spoilerTags = new ArrayList<>();
    private static final String SDPATH = "./ShowdownData/";
    public static JSONObject wikijson;
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
    public static boolean expEdited = false;
    @SuppressWarnings("CanBeFinal")
    public static boolean checkBST = false;
    protected static List<Long> cultists = Arrays.asList(175910318608744448L, 452575044070277122L, 535095576136515605L, 456821278653808650L, 598199247124299776L);
    protected static String tradesid;
    protected static List<String> balls;
    protected static List<String> mons;


    protected final List<Long> allowedGuilds;
    protected final HashSet<String> aliases = new HashSet<>();
    protected final HashMap<String, String> overrideHelp = new HashMap<>();
    protected final HashMap<Long, List<Long>> overrideChannel = new HashMap<>();
    protected final String name;
    protected final String help;
    protected final CommandCategory category;
    protected boolean wip = false;
    protected boolean everywhere = false;
    protected Predicate<Member> allowsMember = m -> false;
    protected boolean customPermissions = false;


    public Command(String name, String help, CommandCategory category, long... guilds) {
        this.name = name;
        this.help = help;
        this.category = category;
        allowedGuilds = guilds.length == 0 ? new ArrayList<>() : Arrays.stream(guilds).boxed().collect(Collectors.toCollection(ArrayList::new));
        commands.add(this);
    }

    public Command(String name, String help, CommandCategory category, List<Long> guilds) {
        this(name, help, category, guilds.stream().mapToLong(l -> l).toArray());
    }

    public Command(String name, String help, CommandCategory category, Command guildBase) {
        this(name, help, category, guildBase.allowedGuilds);
    }

    public static File invertImage(String mon, boolean shiny) {
        BufferedImage inputFile;
        System.out.println("mon = " + mon);
        try {
            File f = new File("../Showdown/sspclient/sprites/dex" + (shiny ? "-shiny" : "") + "/" + mon.toLowerCase() + ".png");
            System.out.println("f.getAbsolutePath() = " + f.getAbsolutePath());
            inputFile = ImageIO.read(f);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        for (int x = 0; x < inputFile.getWidth(); x++) {
            for (int y = 0; y < inputFile.getHeight(); y++) {
                int rgba = inputFile.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(),
                        255 - col.getGreen(),
                        255 - col.getBlue());
                inputFile.setRGB(x, y, col.getRGB());
            }
        }

        try {
            File outputFile = new File("tempimages/invert-" + mon + ".png");
            ImageIO.write(inputFile, "png", outputFile);
            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void loadJarvisPlaylist(String gid) {
        Guild g = jda.getGuildById(gid);
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        playerManager.loadItemOrdered(musicManager, "https://www.youtube.com/playlist?list=PLrwrdAXSpHC5Mr2zC-q_dWKONVybk6JO6", new AudioLoadResultHandler() {


            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                ArrayList<AudioTrack> list = new ArrayList<>(playlist.getTracks());
                Collections.shuffle(list);
                for (AudioTrack audioTrack : list) {
                    play(g, musicManager, audioTrack);
                }
            }

            @Override
            public void noMatches() {
                sendToMe("Jarvis Playlist not found!");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
                sendStacktraceToMe(exception);
            }
        });
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

    public static Command byName(String name) {
        return commands.stream().filter(c -> c.name.equals(name)).findFirst().orElse(null);
    }

    public static void loadAndPlay(final TextChannel channel, final String track, Member mem, String cm) throws IllegalArgumentException {
        if (track.startsWith("https://youtube.com/playlist")) {
            loadPlaylist(channel, track, mem, cm);
            return;
        }
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        System.out.println(track);
        String url;
        YTDataLoader loader;
        if (track.startsWith("https://www.youtube.com/")) {
            url = track;
            loader = new YTDataLoader(Google.getVidByURL(url, false));
        } else {
            SearchResult s = Google.getVidByQuery(track, false);
            url = "https://www.youtube.com/watch?v=" + s.getId().getVideoId();
            loader = new YTDataLoader(s);
        }

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
                            .setTitle(track.getInfo().title, url)
                            .setAuthor("Added to queue", null, mem.getUser().getEffectiveAvatarUrl())
                            .addField("Channel", loader.getChannel(), true)
                            .addField("Song Duration", formatToTime(track.getDuration()), true)
                            .addField("Estimated time until playing", formatToTime(duration), true)
                            .addField("Position in queue", (musicManager.scheduler.queue.size() > 0 ? musicManager.scheduler.queue.size() + 1 : 0) + "", false)
                            .setThumbnail(loader.getThumbnail());
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

    public static <E> List<E> arrayToList(JSONArray arr, Class<E> c) {
        ArrayList<E> list = new ArrayList<>();
        for (Object o : arr) {
            list.add(c.cast(o));
        }
        return list;
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
        if (mutedRoles.containsKey(g.getIdLong()))
            g.addRoleToMember(mem, g.getRoleById(mutedRoles.get(g.getIdLong()))).queue();
        long expires = System.currentTimeMillis() + time * 1000;
        muteTimer(g, expires, mem.getId());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        Database.insert("mutes", "userid, modid, guildid, reason, expires", mem.getIdLong(), mod.getIdLong(), g.getIdLong(), reason, new Timestamp(expires));
    }

    public static void muteTimer(Guild g, long expires, String mem) {
        try {
            if (expires > -1) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        String tstr = new Timestamp(expires).toString();
                        String query = "delete from mutes where userid=" + mem + " and expires='" + tstr.substring(0, Math.min(tstr.length(), 20)) + "0'";
                        System.out.println(query);
                        if (Database.update(query) != 0) {
                            if (mutedRoles.containsKey(g.getIdLong()))
                                g.removeRoleFromMember(mem, g.getRoleById(mutedRoles.get(g.getIdLong()))).queue();
                        }
                    }
                }, new Date(expires));
            }
        } catch (IllegalArgumentException ignored) {
            String tstr = new Timestamp(expires).toString();
            if (Database.update("delete from mutes where userid=" + mem + " and expires='" + tstr.substring(0, Math.min(tstr.length(), 20)) + "0'") != 0) {
                if (mutedRoles.containsKey(g.getIdLong()))
                    g.removeRoleFromMember(mem, g.getRoleById(mutedRoles.get(g.getIdLong()))).queue();
            }
        }
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

    public static void ban(TextChannel tco, Member mod, Member mem, String reason) {
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
        Database.insert("bans", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
    }

    public static void mute(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        long gid = g.getIdLong();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht muten!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        if (mutedRoles.containsKey(gid)) {
            g.addRoleToMember(mem, g.getRoleById(mutedRoles.get(gid))).queue();
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        Database.insert("mutes", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
    }

    public static void unmute(TextChannel tco, Member mem) {
        JSONObject json = getEmolgaJSON().getJSONObject("mutes");
        Guild g = tco.getGuild();
        long gid = g.getIdLong();
        if (mutedRoles.containsKey(gid)) {
            g.removeRoleFromMember(mem, g.getRoleById(mutedRoles.get(gid))).queue();
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde entmutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        tco.sendMessage(builder.build()).queue();
        Database.update("delete from mutes where guildid=" + tco.getGuild().getId() + " and userid=" + mem.getId());
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
        long expires = System.currentTimeMillis() + time * 1000;
        banTimer(g, expires, mem.getId());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gebannt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        Database.insert("bans", "userid, modid, guildid, reason, expires", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason, new Timestamp(expires));
    }

    public static void banTimer(Guild g, long expires, String mem) {
        try {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String tstr = new Timestamp(expires).toString();
                    if (Database.update("delete from bans where userid=" + mem + " and expires='" + tstr.substring(0, Math.min(tstr.length(), 20)) + "0'") != 0) {
                        g.unban(mem).queue();
                        System.out.println("Unbanned!");
                    }
                }
            }, new Date(expires));
        } catch (IllegalArgumentException ignored) {
            if (Database.update("delete from bans where timestamp'" + new Timestamp(expires) + "'") != 0) {
                g.unban(mem).queue();
                System.out.println("Unbanned!");
            }
        }
    }

    public static void warn(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht warnen!");
            tco.sendMessage(builder.build()).queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde verwarnt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessage(builder.build()).queue();
        Database.insert("warns", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
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

    public static int aslIndexPick(ArrayList<String> picks, String mon) {
        for (String pick : picks) {
            if (pick.equalsIgnoreCase(mon) || pick.substring(2).equalsIgnoreCase(mon)) return picks.indexOf(pick);
            if (pick.equalsIgnoreCase("Amigento") && mon.contains("Amigento")) return picks.indexOf(pick);
        }
        return -1;
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
            b.append(method).append(": ").append(jda.getGuildById("745934535748747364").getEmoteById(m.getString("emote")).getAsMention()).append("\n");
            for (String s : shinycountjson.getString("userorder").split(",")) {
                b.append(names.getString(s)).append(": ").append(m.optInt(s, 0)).append("\n");
            }
            b.append("\n");
        }
        jda.getTextChannelById("778380440078647296").editMessageById("778380596413464676", b.toString()).queue();
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
        List<List<Object>> formula = Google.get(sid, tablename + "!F14:M21", true, false);
        List<List<Object>> points = Google.get(sid, tablename + "!F14:M21", false, false);
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
        for (int j = 0; j < 8; j++) {
            sendname.add(namap.get(j));
        }
        System.out.println(sendname);
        RequestBuilder.updateAll(sid, tablename + "!F14", sendname, false);
        System.out.println("Done!");
    }

    public static void sortWooloo(String sid, JSONObject league) {
        int i;
        List<List<Object>> formula = Google.get(sid, "Tabelle!D3:P31", true, false);
        List<List<Object>> p = Google.get(sid, "Tabelle!E3:P31", false, false);
        //List<List<Object>> pf = Google.get(sid, "Tabelle!H3:H39", true, false);
        ArrayList<List<Object>> points = new ArrayList<>();
        for (i = 0; i < 32; i++) {
            if ((i + 4) % 4 == 0) {
                points.add(p.get(i));
            }
        }
        //System.out.println(points);
        List<List<Object>> orig = new ArrayList<>(points);
        JSONObject docnames = getEmolgaJSON().getJSONObject("docnames");
        points.sort((o1, o2) -> {
            int compare = compareColumns(o1, o2, 3, 11, 9);
            if (compare != 0) return compare;
            if (!league.has("results")) return 0;
            JSONObject results = league.getJSONObject("results");
            String n1 = docnames.getString((String) o1.get(0));
            String n2 = docnames.getString((String) o2.get(0));
            if (results.has(n1 + ":" + n2)) {
                return results.getString(n1 + ":" + n2).equals(n1) ? 1 : -1;
            }
            if (results.has(n2 + ":" + n1)) {
                return results.getString(n2 + ":" + n1).equals(n1) ? 1 : -1;
            }
            return 0;
        });
        Collections.reverse(points);
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        HashMap<Integer, String> stMap = new HashMap<>();
        i = 0;
        for (List<Object> objects : orig) {
            List<Object> list = formula.get(i * 4);
            int index = points.indexOf(objects);
            Object logo = list.remove(0);
            Object name = list.remove(0);
            list.subList(0, 2).clear();
            valmap.put(index, list);
            namap.put(index, Arrays.asList(logo, name));
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            senddata.add(valmap.get(j));
            sendname.add(namap.get(j));
            for (int k = 0; k < 3; k++) {
                senddata.add(Collections.emptyList());
                sendname.add(Collections.emptyList());
            }
        }
        RequestBuilder b = new RequestBuilder(sid);
        b.addAll("Tabelle!H3", senddata);
        b.addAll("Tabelle!D3", sendname);
        b.execute();
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

    public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        AudioManager audioManager = guild.getAudioManager();
        Member mem = guild.retrieveMemberById(Constants.FLOID).complete();
        if (!audioManager.isConnected()) {
            if (mem.getVoiceState().inVoiceChannel()) {
                audioManager.openAudioConnection(mem.getVoiceState().getChannel());
            } else {
                sendToMe("Du musst dich in einem Voicechannel befinden!");
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
            if (f.createNewFile()) {
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

    public static JSONObject loadSD(String path, int sub) {
        try {
            File f = new File(path);
            System.out.println("path = " + path);
            List<String> l = Files.readAllLines(f.toPath());
            int i = 0;
            StringBuilder b = new StringBuilder();
            boolean func = false;
            //int braces = 0;
            for (String s : l) {
                String str;
                if (i == 0)
                    str = s.substring(sub);
                else if (i == l.size() - 1) str = s.substring(0, s.length() - 1);
                else str = s;
                if (path.endsWith("moves.ts")) {
                    /*if ((str.contains("{") && str.contains("(") && str.contains(")") && !str.contains(":")) || str.equals("\t\tcondition: {") || str.equals("\t\tsecondary: {")) {
                        func = true;
                    }*/
                    if (str.startsWith("\t\t") && (str.endsWith("{") || str.endsWith("["))) func = true;
                    if (str.equals("\t\t},") && func) {
                        func = false;
                    } else if (!str.startsWith("\t\t\t") && !func) b.append(str).append("\n");
                } else {
                    b.append(str).append("\n");
                }
                i++;
            }
            if (path.endsWith("moves.ts")) {
                //BufferedWriter writer = new BufferedWriter(new FileWriter("ichbineinwirklichtollertest.json"));
                BufferedWriter writer2 = new BufferedWriter(new FileWriter("ichbineinbesserertest.txt"));
                //writer.write(object.toString(4));
                writer2.write(b.toString());
                //writer.close();
                writer2.close();
            }
            return new JSONObject(b.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getDataJSON(String mod) {
        return ModManager.getByName(mod).getDex();
    }

    public static JSONObject getDataJSON() {
        return getDataJSON("default");
    }

    public static JSONObject getTypeJSON(String mod) {
        return ModManager.getByName(mod).getTypechart();
    }

    public static JSONObject getTypeJSON() {
        return getTypeJSON("default");
    }

    public static JSONObject getLearnsetJSON(String mod) {
        return ModManager.getByName(mod).getLearnsets();
    }

    public static JSONObject getMovesJSON(String mod) {
        return ModManager.getByName(mod).getMoves();
    }

    public static String getModByGuild(GuildCommandEvent e) {
        String gid = e.getGuild().getId();
        if ("447357526997073930".equals(gid) || "786351922029527130".equals(gid)) {
            return "nml";
        }
        return "default";
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

    public static synchronized void saveEmolgaJSON() {
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
        new MusicCommand();
        new NpCommand();
        new PlayCommand();
        new QlCommand();
        new QueueCommand();
        new SkipCommand();
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
        new BanCommand();
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
        new RandomPickCommand();
        new CalcCommand();
        new LeaderboardCommand();
        new InviteCommand();
        new ResetCooldownCommand();
        new SpoilerTagsCommand();
        new AnalyseCommand();
        new SearchReplaysCommand();
        new InviteUrlCommand();
        new FloHelpCommand();
        new UserInfoCommand();
        new EmolgaDiktaturCommand();
        new UserInfoCommand();
        new ServerInfoCommand();
        new SkipPickCommand();
        new PokeFansExportCommand();
        new RevolutionCommand();
        new ResetRevolutionCommand();
        new NatureCommand();
        new InvertColorsCommand();
        new ResistanceCommand();
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
        loadJSONFiles();
        JSONObject mute = emolgajson.getJSONObject("mutedroles");
        for (String s : mute.keySet()) {
            mutedRoles.put(Long.parseLong(s), mute.getLong(s));
        }
        JSONObject mod = emolgajson.getJSONObject("moderatorroles");
        for (String s : mod.keySet()) {
            moderatorRoles.put(Long.parseLong(s), mod.getLong(s));
        }
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
        emolgachannel.put(Constants.ASLID, new ArrayList<>(Arrays.asList(728680506098712579L, 736501675447025704L)));
        emolgachannel.put(Constants.BSID, new ArrayList<>(Arrays.asList(732545253344804914L, 735076688144105493L)));
        emolgachannel.put(709877545708945438L, new ArrayList<>(Collections.singletonList(738893933462945832L)));
        emolgachannel.put(677229415629062180L, new ArrayList<>(Collections.singletonList(731455491527540777L)));
        emolgachannel.put(694256540642705408L, new ArrayList<>(Collections.singletonList(695157832072560651L)));
        emolgachannel.put(747357029714231299L, new ArrayList<>(Arrays.asList(752802115096674306L, 762411109859852298L)));
        sdAnalyser.put(Constants.ASLID, (game, uid1, uid2, kills, deaths, args) -> {
            JSONObject league = null;
            JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
            for (String s : drafts.keySet()) {
                if (s.equalsIgnoreCase("IchMagKekse")) continue;
                JSONObject o = drafts.getJSONObject(s);
                if (Long.parseLong(o.optString("guild")) != Constants.ASLID) continue;
                if (o.getString("table").contains(uid1)) {
                    league = o;
                    System.out.println("Conference: " + s);
                    break;
                }
            }
            if (league == null) return;
            List<String> table = Arrays.asList(league.getString("table").split(","));
            System.out.println("table = " + table);
            int tin1 = table.indexOf(uid1);
            int tin2 = table.indexOf(uid2);
            System.out.println("tin1 = " + tin1);
            System.out.println("tin2 = " + tin2);
            String sid = league.getString("sid");
            int gameday = getGameDay(league, uid1, uid2);
            if (gameday == -1) {
                System.out.println("GAMEDAY -1");
                return;
            }
            if (!league.has("results")) league.put("results", new JSONObject());
            JSONObject results = league.getJSONObject("results");
            if (results.has(uid1 + ":" + uid2) || results.has(uid2 + ":" + uid1)) {
                sendToMe("Double Entry -> skipped");
                return;
            }
            ArrayList<String> userids = new ArrayList<>(Arrays.asList(uid1, uid2));
            RequestBuilder b = new RequestBuilder(sid);
            int aliveP1 = 0;
            int aliveP2 = 0;
            for (SDPokemon p : game[0].getMons()) {
                if (!p.isDead()) aliveP1++;
            }
            for (SDPokemon p : game[1].getMons()) {
                if (!p.isDead()) aliveP2++;
            }
            String str = null;
            int battleindex = -1;
            List<String> battleorder = Arrays.asList(league.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";"));
            for (String s : battleorder) {
                if (s.contains(uid1)) {
                    str = s;
                    battleindex = battleorder.indexOf(s);
                    break;
                }
            }
            gameday--;
            ArrayList<String>[] mons = (ArrayList<String>[]) args[0];
            List<Object> res;
            if (str.split(":")[0].equals(uid1)) {
                res = Arrays.asList(String.valueOf(aliveP1), ":", String.valueOf(aliveP2));
            } else {
                res = Arrays.asList(String.valueOf(aliveP2), ":", String.valueOf(aliveP1));
            }
            System.out.println(res);
            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 5) + (gameday * 10 + 5), res);
            boolean p1wins = false;
            for (int i = 0; i < 2; i++) {
                String uid = userids.get(i);
                ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                int x = 0;
                int index = table.indexOf(uid);
                List<List<Object>> list = Google.get(sid, "Teams!" + getAsXCoord((index > 3 ? index - 4 : index) * 5 + 3) + (index > 3 ? 24 : 7)
                        + ":" + getAsXCoord((index > 3 ? index - 4 : index) * 5 + 4) + (index > 3 ? 35 : 18), true, false);
                System.out.println("gameday = " + gameday);
                if (str.split(":")[0].equals(uid)) {
                    System.out.println("LINKS Picks von " + game[i].getNickname() + ": " + picks);
                    p1wins = game[i].isWinner();
                    for (String s : mons[i]) {
                        System.out.println("s = " + s + " ");
                        int monindex = aslIndexPick(picks, s);
                        if (monindex > -1) {
                            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 3) + (gameday * 10 + 6 + x), Arrays.asList(deaths.get(s), kills.get(s), s));
                            List<Object> l = list.get(monindex);
                            b.addRow("Teams!" + getAsXCoord((index > 3 ? index - 4 : index) * 5 + 3) + ((index > 3 ? 24 : 7) + monindex),
                                    Arrays.asList(l.get(0) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 4) + (gameday * 10 + 6 + x),
                                            l.get(1) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 3) + (gameday * 10 + 6 + x)));
                            x++;
                        }
                    }
                } else {
                    System.out.println("RECHTS Picks von " + game[i].getNickname() + ": " + picks);
                    for (String s : mons[i]) {
                        System.out.println("s = " + s);
                        int monindex = aslIndexPick(picks, s);
                        if (monindex > -1) {
                            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 7) + (gameday * 10 + 6 + x), Arrays.asList(s, kills.get(s), deaths.get(s)));
                            List<Object> l = list.get(monindex);
                            b.addRow("Teams!" + getAsXCoord((index > 3 ? index - 4 : index) * 5 + 3) + ((index > 3 ? 24 : 7) + monindex),
                                    Arrays.asList(l.get(0) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 8) + (gameday * 10 + 6 + x),
                                            l.get(1) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 9) + (gameday * 10 + 6 + x)));
                            x++;
                        }
                    }
                }
                if (game[i].isWinner()) {
                    results.put(uid1 + ":" + uid2, uid);
                    b.addSingle("Daten!D" + (index + 1), (Integer.parseInt((String) Google.get(sid, "Daten!D" + (index + 1), false, false).get(0).get(0)) + 1));
                } else {
                    b.addSingle("Daten!E" + (index + 1), (Integer.parseInt((String) Google.get(sid, "Daten!E" + (index + 1), false, false).get(0).get(0)) + 1));
                }
            }
            b.addSingle("Spielplan!" + getAsXCoord(battleindex * 8 + 6) + (gameday * 10 + 3), "=HYPERLINK(\"" + args[1] + "\"; \"VS\")");
            b.execute();
            saveEmolgaJSON();
            sortASL(league);
            gameday++;
            evaluatePredictions(league, p1wins, gameday, uid1, uid2);
        });
        sdAnalyser.put(709877545708945438L, (game, uid1, uid2, kills, deaths, args) -> {
            Guild guild = jda.getGuildById("709877545708945438");
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
        sdAnalyser.put(736555250118295622L, (game, uid1, uid2, kills, deaths, args) -> {
            // ZBS
            JSONObject league;
            JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
            int l;
            if (drafts.getJSONObject("WoolooCupS3L1").getString("table").contains(uid1)) l = 1;
            else l = 2;
            league = drafts.getJSONObject("WoolooCupS3L" + l);
            String sid = league.getString("sid");
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
                List<List<Object>> get = Google.get(sid, "Teamübersicht!" + getAsXCoord((index > 3 ? index - 4 : index) * 6 + 4) + (index > 3 ? 26 : 7) + ":"
                        + getAsXCoord((index > 3 ? index - 4 : index) * 6 + 5) + (index > 3 ? 38 : 19), false, false);
                List<List<Object>> list = new ArrayList<>();
                int x = 0;
                for (String pick : picks) {
                    String kill = getNumber(kills, pick);
                    String death = getNumber(deaths, pick);
                    list.add(Arrays.asList((kill.equals("") ? 0 : Integer.parseInt(kill)) + Integer.parseInt((String) get.get(x).get(0)), (death.equals("") ? 0 : Integer.parseInt(death)) + Integer.parseInt((String) get.get(x).get(1))));
                    x++;
                }
                List<List<Object>> getvic;
                try {
                    getvic = Google.get(sid, "Daten!D" + (index + 1) + ":E" + (index + 1), false, false);
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                    return;
                }
                int win = Integer.parseInt((String) getvic.get(0).get(0));
                int loose = Integer.parseInt((String) getvic.get(0).get(1));
                if (game[i].isWinner()) {
                    win++;
                    if (!league.has("results"))
                        league.put("results", new JSONObject());
                    league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                } else loose++;
                try {
                    b.addAll("Teamübersicht!" + getAsXCoord((index > 3 ? index - 4 : index) * 6 + 4) + (index > 3 ? 26 : 7), list);
                    b.addRow("Daten!D" + (index + 1), Arrays.asList(win, loose));
                } catch (IllegalArgumentException IllegalArgumentException) {
                    IllegalArgumentException.printStackTrace();
                }
                i++;
            }
            generateResult(b, game, league, gameday, uid1, "Spielplan", "Wooloo", (String) args[1]);
            b.execute();
            sortWooloo(sid, league);
            saveEmolgaJSON();
        });
    }

    public static void evaluatePredictions(JSONObject league, boolean p1wins, int gameday, String uid1, String uid2) {
        JSONObject predictiongame = league.getJSONObject("predictiongame");
        JSONObject gd = predictiongame.getJSONObject("ids").getJSONObject(String.valueOf(gameday));
        String key = gd.has(uid1 + ":" + uid2) ? uid1 + ":" + uid2 : uid2 + ":" + uid1;
        Message message = jda.getTextChannelById(predictiongame.getLong("channelid")).retrieveMessageById(gd.getLong(key)).complete();
        List<User> e1 = message.retrieveReactionUsers(jda.getEmoteById(540970044297838597L)).complete();
        List<User> e2 = message.retrieveReactionUsers(jda.getEmoteById(645622238757781505L)).complete();
        if (p1wins) {
            for (User user : e1) {
                if (!e2.contains(user)) {
                    Database.incrementPredictionCounter(user.getIdLong());
                }
            }
        } else {
            for (User user : e2) {
                if (!e1.contains(user)) {
                    Database.incrementPredictionCounter(user.getIdLong());
                }
            }
        }
    }

    public static void generateResult(RequestBuilder b, Player[] game, JSONObject league, int gameday, String uid1, String sheet, String leaguename, String replay) {
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
            b.addSingle(sheet + "!" + coords, "=HYPERLINK(\"" + replay + "\"; \"" + aliveP1 + ":" + aliveP2 + "\")");
            //b.addSingle(sheet + "!" + coords, aliveP1 + ":" + aliveP2);
        } else {
            b.addSingle(sheet + "!" + coords, "=HYPERLINK(\"" + replay + "\"; \"" + aliveP2 + ":" + aliveP1 + "\")");
            //b.addSingle(sheet + "!" + coords, aliveP2 + ":" + aliveP1);
        }
    }

    public static List<String> getMonList(String mod) {
        return getDataJSON(mod).keySet().stream().filter(s -> !s.endsWith("gmax")).collect(Collectors.toList());
    }

    public static void woolooStyle(String sid, Message message, String uid1, String uid2) {
        Guild guild = jda.getGuildById("709877545708945438");
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
        System.out.println("gameday = " + gameday);
        System.out.println("index = " + index);
        if (gameday < 4) return "C" + (gameday * 5 + index - 1);
        if (gameday < 7) return "F" + ((gameday - 3) * 5 + index - 1);
        return "I" + ((gameday - 6) * 5 + index - 1);
    }

    private static String getASLGameplanCoords(int gameday, int index, int pk) {
        return getAsXCoord(gameday * 5 - 3) + (index * 6 + pk + 3);
    }

    public static void loadJSONFiles() {
        emolgajson = load("./emolgadata.json");
        wikijson = load("./wikidata.json");
        //datajson = loadSD("pokedex.ts", 59);
        //movejson = loadSD("learnsets.ts", 62);
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
        return commands.stream().filter(c -> c.category == category && c.allowsGuild(g) && c.allowsMember(mem)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void updatePresence() {
        ResultSet set = Database.select("select count(*) as total from analysis");
        try {
            set.next();
            jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("in " + set.getInt("total") + " Replay-Channel"));
        } catch (Exception ex) {
            sendStacktraceToMe(ex);
        }
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
        long gid = e.getGuild().getIdLong();
        for (Command command : commands) {
            if (!command.checkPrefix(msg)) continue;
            if (mem.getIdLong() != Constants.FLOID) {
                if (command.category == CommandCategory.Flo) return;
                if (!command.category.disabled.isEmpty()) {
                    tco.sendMessage(command.category.disabled).queue();
                }
                if (command.wip) {
                    tco.sendMessage("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!").queue();
                    return;
                }
            }
            PermissionCheck check = command.checkPermissions(gid, mem);
            if (check == PermissionCheck.NOGUILD) break;
            if (check == PermissionCheck.NOPERMISSION) {
                tco.sendMessage(NOPERM).queue();
                break;
            }
            if (!command.everywhere && !command.category.isEverywhere()) {
                if (command.overrideChannel.containsKey(gid)) {
                    List<Long> l = command.overrideChannel.get(gid);
                    if (!l.contains(e.getChannel().getIdLong())) {
                        e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                        return;
                    }
                } else {
                    if (emolgachannel.containsKey(gid)) {
                        List<Long> l = emolgachannel.get(gid);
                        if (!l.contains(e.getChannel().getIdLong())) {
                            e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                            return;
                        }
                    }
                }
            }
            try {
                command.process(new GuildCommandEvent(e));
            } catch (Exception ex) {
                ex.printStackTrace();
                tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo/TecToast.\n" + command.getHelp(e.getGuild()) + (mem.getIdLong() == Constants.FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
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
        if (s.contains("--phys")) return "Physical";
        else if (s.contains("--spez")) return "Special";
        else if (s.contains("--status")) return "Status";
        return "";
    }

    public static List<JSONObject> getAllForms(String monname, String mod) {
        JSONObject json = getDataJSON(mod);
        JSONObject mon = json.getJSONObject(getSDName(monname));
        //System.out.println("getAllForms mon = " + mon.toString(4));
        if (!mon.has("formeOrder")) return Collections.singletonList(mon);
        return mon.getJSONArray("formeOrder").toList().stream().map(o -> toSDName((String) o)).filter(json::has).map(json::getJSONObject).collect(Collectors.toList());
        //return json.keySet().stream().filter(s -> s.startsWith(monname.toLowerCase()) && !s.endsWith("gmax") && (s.equalsIgnoreCase(monname) || json.getJSONObject(s).has("forme"))).sorted(Comparator.comparingInt(String::length)).map(json::getJSONObject).collect(Collectors.toList());
    }

    public static ArrayList<String> getAttacksFrom(String pokemon, String msg, String form, String mod) {
        return getAttacksFrom(pokemon, msg, form, 8, mod);
    }

    public static boolean moveFilter(String msg, String move) {
        JSONObject o = getWikiJSON().getJSONObject("movefilter");
        for (String s : o.keySet()) {
            if (msg.toLowerCase().contains("--" + s) && !o.getJSONArray(s).toList().contains(move)) return false;
        }
        return true;
    }

    public static ArrayList<String> getAttacksFrom(String pokemon, String msg, String form, int maxgen, String mod) {
        ArrayList<String> already = new ArrayList<>();
        String type = getType(msg);
        String dmgclass = getClass(msg);
        JSONObject movejson = getLearnsetJSON(mod);
        JSONObject json = getWikiJSON();
        JSONObject atkdata = getMovesJSON(mod);
        JSONObject data = getDataJSON(mod);
        try {
            String str = getSDName(pokemon) + (form.equals("Normal") ? "" : form.toLowerCase());
            while (str != null) {
                JSONObject learnset = movejson.getJSONObject(str).getJSONObject("learnset");
                ResultSet set = getTranslationList(learnset.keySet(), mod);
                while (set.next()) {
                    //System.out.println("moveengl = " + moveengl);
                    String moveengl = set.getString("englishid");
                    String move = set.getString("germanname");
                    //System.out.println("move = " + move);
                    if ((type.equals("") || atkdata.getJSONObject(moveengl).getString("type").equals(getEnglName(type))) &&
                            (dmgclass.equals("") || atkdata.getJSONObject(moveengl).getString("category").equals(dmgclass)) &&
                            (!msg.toLowerCase().contains("--prio") || atkdata.getJSONObject(moveengl).getInt("priority") > 0) &&
                            (containsGen(learnset, moveengl, maxgen)) &&
                            moveFilter(msg, move) &&
                            !already.contains(move)) {
                        already.add(move);
                    }
                }
                JSONObject mon = data.getJSONObject(str);
                if (mon.has("prevo")) {
                    String s = mon.getString("prevo");
                    if (s.endsWith("-Alola") || s.endsWith("-Galar") || s.endsWith("-Unova"))
                        str = s.replaceAll("-", "").toLowerCase();
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

    public static void sendStacktraceToMe(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Command.sendToMe(sw.toString());
    }

    public static void sendToUser(Member mem, String msg) {
        sendToUser(mem.getUser(), msg);
    }

    public static void sendToUser(User user, String msg) {
        user.openPrivateChannel().flatMap(pc -> pc.sendMessage(msg)).queue();
    }

    public static void sendToUser(long id, String msg) {
        jda.retrieveUserById(id).flatMap(User::openPrivateChannel).flatMap(pc -> pc.sendMessage(msg)).queue();
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
            jda.getGuildById(Constants.BSID).retrieveMembersByIds(players.toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
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

    public static void analyseASLReplay(long authorid, String url, JDA jda) {
        long gid = Constants.ASLID;
        Guild g = jda.getGuildById(gid);
        System.out.println(url);
        Player[] game;
        try {
            game = Analysis.analyse(url);
        } catch (Exception ex) {
            ex.printStackTrace();
            sendToMe("Analyse Error! Console!");
            sendToUser(authorid, "Beim Auswerten des Replays ist ein Fehler aufgetreten! Flo wurde benachrichtigt, du musst nichts machen.");
            return;
        }
        System.out.println("Analysed in PN!");
        String u1 = game[0].getNickname();
        String u2 = game[1].getNickname();
        String name1;
        String name2;
        String uid1 = null;
        String uid2 = null;
        JSONObject json = getEmolgaJSON();
        if (json.getJSONObject("showdown").has(String.valueOf(gid))) {
            JSONObject showdown = json.getJSONObject("showdown").getJSONObject(String.valueOf(gid));
            System.out.println("u1 = " + u1);
            System.out.println("u2 = " + u2);
            for (String s : showdown.keySet()) {
                if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
            }
            name1 = uid1 != null && gid == 518008523653775366L ? uid1.equals("LSD") ? "REPLACELSD" : jda.getGuildById(gid).retrieveMemberById(uid1).complete().getEffectiveName() : game[0].getNickname();
            name2 = uid2 != null && gid == 518008523653775366L ? uid2.equals("LSD") ? "REPLACELSD" : jda.getGuildById(gid).retrieveMemberById(uid2).complete().getEffectiveName() : game[1].getNickname();
        } else {
            name1 = game[0].getNickname();
            name2 = game[1].getNickname();
        }
        if (uid1 == null || uid2 == null) return;
        JSONObject league = null;
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        for (String s : drafts.keySet()) {
            if (s.equalsIgnoreCase("IchMagKekse")) continue;
            JSONObject o = drafts.getJSONObject(s);
            if (Long.parseLong(o.optString("guild")) != Constants.ASLID) continue;
            if (o.getString("table").contains(uid1)) {
                league = o;
                System.out.println("Conference: " + s);
                break;
            }
        }
        if (league == null) return;
        int lastday = league.getJSONObject("predictiongame").getInt("lastDay");
        int gameday = getGameDay(league, uid1, uid2);
        if (lastday < gameday) {
            sendToUser(authorid, "Das Replay wurde gespeichert, aber noch nicht reingeschickt, da es zu einem späteren Spieltag gehört!");
            JSONObject early = json.getJSONObject("earlyreplays");
            if (!early.has(String.valueOf(gameday))) early.put(String.valueOf(gameday), new JSONArray());
            early.getJSONArray(String.valueOf(gameday)).put(url);
            saveEmolgaJSON();
            return;
        }
        System.out.println("uid1 = " + uid1);
        System.out.println("uid2 = " + uid2);
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
        ArrayList<String> p1mons = new ArrayList<>();
        ArrayList<String> p2mons = new ArrayList<>();
        boolean spoiler = spoilerTags.contains(gid);
        if (spoiler) t1.append("||");
        for (SDPokemon p : game[0].getMons()) {
            String monName = getMonName(p.getPokemon(), gid);
            kills.put(monName, String.valueOf(p.getKills()));
            deaths.put(monName, p.isDead() ? "1" : "0");
            p1mons.add(monName);
            t1.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (p1wins || spoiler) ? "X" : "").append("\n");
        }
        if (spoiler) t1.append("||");
        if (spoiler) t2.append("||");
        for (SDPokemon p : game[1].getMons()) {
            String monName = getMonName(p.getPokemon(), gid);
            kills.put(monName, String.valueOf(p.getKills()));
            deaths.put(monName, p.isDead() ? "1" : "0");
            p2mons.add(monName);
            t2.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (!p1wins || spoiler) ? "X" : "").append("\n");
        }
        if (spoiler) t2.append("||");
        System.out.println("Kills");
        System.out.println(kills);
        System.out.println("Deaths");
        System.out.println(deaths);

        String str;
        if (spoiler) {
            str = name1 + " ||" + winloose + "|| " + name2 + "\n\n" + name1 + ":\n" + t1.toString()
                    + "\n" + name2 + ": " + "\n" + t2.toString();
        } else {
            str = name1 + " " + winloose + " " + name2 + "\n\n" + name1 + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1.toString()
                    + "\n" + name2 + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2.toString();
        }

        g.getTextChannelById(league.getLong("replaychannel")).sendMessage("Spieltag " + gameday + "\n" + name1 + " vs " + name2 + "\n" + url).queue();
        TextChannel resultchannel = g.getTextChannelById(league.getLong("resultchannel"));
        resultchannel.sendMessage(str).queue();
        for (int i = 0; i < 2; i++) {
            if (game[i].getMons().stream().anyMatch(mon -> mon.getPokemon().equals("Zoroark") || mon.getPokemon().equals("Zorua")))
                resultchannel.sendMessage("Im Team von " + game[i].getNickname() + " befindet sich ein Zorua/Zoroark! Bitte noch einmal die Kills von ihm überprüfen!").queue();
        }
        System.out.println("In Emolga Listener!");
        if (!json.getJSONObject("showdown").has(String.valueOf(gid))) return;
        if (sdAnalyser.containsKey(gid)) {
            sdAnalyser.get(gid).analyse(game, uid1, uid2, kills, deaths, new ArrayList[]{p1mons, p2mons}, url);
        }
    }

    public static void sendEarlyASLReplays(int gameday) {
        JSONObject early = getEmolgaJSON().getJSONObject("earlyreplays");
        if (!early.has(String.valueOf(gameday))) return;
        for (Object o : early.getJSONArray(String.valueOf(gameday))) {
            analyseASLReplay(Constants.FLOID, (String) o, jda);
        }
        early.remove(String.valueOf(gameday));
        saveEmolgaJSON();
    }

    public static String getIconSprite(String str) {
        System.out.println("s = " + str);
        String gitname;
        JSONObject data = getDataJSON();
        if (str.toLowerCase().startsWith("a-")) {
            String sdName = getSDName(str.substring(2));
            if (sdName.equals("")) return "";
            gitname = getWithZeros(data.getJSONObject(sdName).getInt("num"), 3) + "-a";
        } else if (str.toLowerCase().startsWith("g-")) {
            String sdName = getSDName(str.substring(2));
            if (sdName.equals("")) return "";
            gitname = getWithZeros(data.getJSONObject(sdName).getInt("num"), 3) + "-g";
        } else if (serebiiex.keySet().stream().anyMatch(str::equalsIgnoreCase)) {
            String ex = serebiiex.keySet().stream().filter(str::equalsIgnoreCase).collect(Collectors.joining(""));
            String sdName = getSDName(ex.split("-")[0]).substring(5);
            gitname = getWithZeros(data.getJSONObject(sdName).getInt("num"), 3) + serebiiex.get(str);
        } else {
            String sdName = getSDName(str);
            if (sdName.equals("")) return "";
            gitname = getWithZeros(data.getJSONObject(sdName).getInt("num"), 3) + "";
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
            gitname = getWithZeros(data.getJSONObject(getSDName(str.substring(2))).getInt("num"), 3) + "-a";
        } else if (str.startsWith("G-")) {
            gitname = getWithZeros(data.getJSONObject(getSDName(str.substring(2))).getInt("num"), 3) + "-g";
        } else if (serebiiex.containsKey(str)) {
            gitname = getWithZeros(data.getJSONObject(getSDName(str.split("-")[0])).getInt("num"), 3) + serebiiex.get(str);
        } else {
            gitname = getWithZeros(data.getJSONObject(getSDName(str)).getInt("num"), 3) + "";
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

    public static long getXPNeeded(int level) {
        return (long) (5 * (Math.pow(level, 2)) + 50 * level + 100);
    }

    public static int getLevelFromXP(int xp) {
        return getLevelFromXP((long) xp);
    }

    public static int getLevelFromXP(long xp) {
        long remaining_xp = xp;
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

    public static void sortASL(JSONObject league) {
        String sid = league.getString("sid");
        List<List<Object>> formula = Google.get(sid, "Tabelle!B4:J11", true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle!C4:J11", false, false);
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
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            int index = points.indexOf(objects);
            valmap.put(index, formula.get(i));
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        //List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 12; j++) {
            senddata.add(valmap.get(j));
        }
        RequestBuilder.updateAll(sid, "Tabelle!B4", senddata);
        System.out.println("Done!");
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
        RequestBuilder b = new RequestBuilder(sid);
        b.addAll("Tabelle!F3", senddata);
        b.addAll("Tabelle!C3", sendname);
        Request request = new Request();
        request.setUpdateCells(new UpdateCellsRequest().setRows(rowData)
                .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(1702581801).setStartRowIndex(2).setEndRowIndex(20).setStartColumnIndex(2).setEndColumnIndex(3)));
        b.addBatch(request);
        b.execute();
        System.out.println("Done!");
    }

    public static void singleThread(Runnable r) {
        new Thread(r).start();
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
            String ret = gername + "-" + split[1];
            System.out.println("getDraftGerName ret = " + ret);
            return ret;
        }
        return "";
    }

    public static String getGerName(String s) {
        return getGerName(s, "default");
    }

    public static String getGerName(String s, String mod) {
        String check = checkShortcuts(s);
        if (check != null) return check;
        System.out.println("getGerName s = " + s);
        if (serebiiex.keySet().stream().anyMatch(s::equalsIgnoreCase)) {
            String[] ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining("")).split("-");
            return getGerName(ex[0]) + "-" + ex[1];
        }
        ResultSet set = getTranslation(s, mod);
        try {
            if (set.next()) {
                return set.getString("type") + ";" + set.getString("germanname");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return "";
    }

    public static String getGerNameNoCheck(String s) {
        //System.out.println("NoCheckGerName s = " + s);
        ResultSet set = getTranslation(s, null);
        try {
            set.next();
            return set.getString("germanname");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static String getEnglName(String s) {
        return getEnglName(s, "default");
    }

    public static String getEnglName(String s, String mod) {
        String str = getEnglNameWithType(s, mod);
        if (str.equals("")) return "";
        return str.split(";")[1];
    }

    public static String getEnglNameWithType(String s) {
        return getEnglNameWithType(s, "default");
    }

    public static String getEnglNameWithType(String s, String mod) {
        String check = checkShortcuts(s);
        if (check != null) {
            return getEnglNameWithType(check.split(";")[1]);
        }
        ResultSet set = getTranslation(s, mod);
        try {
            if (set.next()) {
                return set.getString("type") + ";" + set.getString("englishname");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return "";
    }

    public static ResultSet getTranslation(String s, String mod) {
        String id = toSDName(s);
        String query = "select * from translations where (englishid=\"" + id + "\" or germanid=\"" + id + "\")" + (mod != null ? " and (modification=\"" + mod + "\"" + (!mod.equals("default") ? " or modification=\"default\"" : "") + ")" : "");
        //System.out.println(query);
        return Database.select(query);
    }

    public static ResultSet getTranslationList(Collection<String> l, String mod) {
        String query = "select * from translations where (" + l.stream().map(str -> "englishid=\"" + toSDName(str) + "\"").collect(Collectors.joining(" or ")) + ")" + (mod != null ? " and (modification=\"" + mod + "\"" + (!mod.equals("default") ? " or modification=\"default\"" : "") + ")" : "");
        return Database.select(query);
    }

    public static String getSDName(String s) {
        return getSDName(s, "default");
    }

    public static String getSDName(String s, String mod) {
        System.out.println("getSDName s = " + s);
        String engl = getEnglNameWithType(s, mod);
        if (engl.equals("")) return "";
        return toSDName(engl.split(";")[1]);
    }

    public static String toSDName(String s) {
        return s.toLowerCase().replaceAll("[^a-zA-Z0-9äöüÄÖÜß]+", "");
    }

    public static String getDataName(String s) {
        System.out.println("s = " + s);
        if (s.equals("Wie-Shu")) return "mienshao";
        if (s.equals("Porygon-Z")) return "porygonz";
        if (s.equals("Sen-Long")) return "drampa";
        if (s.startsWith("Kapu-")) return getSDName(s);
        String[] split = s.split("-");
        if (split.length == 1) return getSDName(s);
        if (s.startsWith("M-")) {
            if (split.length == 3) {
                return getSDName(split[1]) + "mega" + split[2].toLowerCase();
            }
            return getSDName(split[1]) + "mega";
        }
        if (s.startsWith("A-")) return getSDName(split[1]) + "alola";
        if (s.startsWith("G-")) return getSDName(split[1]) + "galar";
        return getSDName(split[0]) + toSDName(sdex.getOrDefault(s, ""));
    }

    public static boolean canLearn(String pokemon, String form, String atk, String msg, int maxgen, String mod) {
        return getAttacksFrom(pokemon, msg, form, maxgen, mod).contains(atk);
    }

    public static String checkShortcuts(String s) {
        return getWikiJSON().getJSONObject("shortcuts").optString(s.toLowerCase(), null);
    }

    public static String getMonName(String s, long gid) {
        return getMonName(s, gid, true);
    }

    public static String getMonName(String s, long gid, boolean withDebug) {
        if (withDebug)
            System.out.println("s = " + s);
        if (gid == 709877545708945438L) {
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
        if (s.equals("Calyrex-Shadow")) return "Coronospa-Rappenreiter";
        if (s.equals("Calyrex-Ice")) return "Coronospa-Schimmelreiter";
        if (s.equals("Shaymin-Sky")) return "Shaymin-Sky";
        if (s.contains("Unown")) return "Icognito";
        if (s.contains("Zacian-Crowned")) return "Zacian-Crowned";
        if (s.contains("Zamazenta-Crowned")) return "Zamazenta-Crowned";
        if (s.equals("Greninja-Ash")) return "Ash-Quajutsu";
        if (s.equals("Zarude-Dada")) return "Zarude";
        if (s.contains("Furfrou")) return "Coiffwaff";
        if (s.contains("Genesect")) return "Genesect";
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
        if (gid == Constants.ASLID && s.contains("Urshifu")) return "Wulaosu-Wasser";
        if (s.contains("Urshifu")) return "Wulaosu";
        if (s.contains("Gourgeist")) return "Pumpdjinn";
        if (s.contains("Pumpkaboo")) return "Irrbis";
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
            else if (split[1].equals("Psychic")) return "Amigento-Psycho";
            else return "Amigento-" + getGerName(split[1]).split(";")[1];
        }
        if (s.contains("Arceus")) {
            String[] split = s.split("-");
            if (split.length == 1 || s.equals("Arceus-*")) return "Arceus";
            else if (split[1].equals("Psychic")) return "Arceus-Psycho";
            else return "Arceus-" + getGerName(split[1]).split(";")[1];
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
        String gername = getGerName(s);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(s.split("-")));
        if (gername.startsWith("pkmn;")) {
            return gername.split(";")[1];
        }
        String first = split.remove(0);
        return getGerName(first).split(";")[0] + String.join("-" + split);
    }

    protected void setCustomPermissions(Predicate<Member> predicate) {
        this.allowsMember = predicate;
        this.customPermissions = true;
    }

    protected void setAdminOnly() {
        setCustomPermissions(PermissionPreset.ADMIN);
    }

    public boolean allowsMember(Member mem) {
        return (category.allowsMember(mem) && !customPermissions) || allowsMember.test(mem);
    }

    public boolean allowsGuild(Guild g) {
        return allowsGuild(g.getIdLong());
    }

    public boolean allowsGuild(long gid) {
        if (gid == Constants.MYSERVER) return true;
        if (allowedGuilds.isEmpty() && category.allowsGuild(gid)) return true;
        return !allowedGuilds.isEmpty() && allowedGuilds.contains(gid);
    }

    public PermissionCheck checkPermissions(long gid, Member mem) {
        if (!allowsGuild(gid)) return PermissionCheck.NOGUILD;
        if (!allowsMember(mem)) return PermissionCheck.NOPERMISSION;
        return PermissionCheck.GRANTED;
    }

    private boolean checkPrefix(String msg) {
        if (category == CommandCategory.Music) {
            return msg.toLowerCase().startsWith("e!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("e!" + s.toLowerCase() + " "))
                    || msg.equalsIgnoreCase("e!" + name) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("e!" + s));
        }
        return msg.toLowerCase().startsWith("!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("!" + s.toLowerCase() + " "))
                || msg.equalsIgnoreCase("!" + name.toLowerCase()) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("!" + s));
    }

    public abstract void process(GuildCommandEvent e) throws Exception;

    public String getName() {
        return name;
    }

    public String getHelp() {
        return help;
    }

    public String getHelp(Guild g) {
        return overrideHelp.getOrDefault(g.getId(), help) + (wip ? " (**W.I.P.**)" : "");
    }

    private enum PermissionCheck {
        GRANTED,
        NOPERMISSION,
        NOGUILD
    }

    public static final class PermissionPreset {
        public static final Predicate<Member> OWNER = Member::isOwner;
        public static final Predicate<Member> ADMIN = member -> member.hasPermission(Permission.ADMINISTRATOR);
        public static final Predicate<Member> MODERATOR = m -> ADMIN.test(m) || m.getRoles().stream().anyMatch(r -> Command.moderatorRoles.containsValue(r.getIdLong()));

        public static Predicate<Member> fromRole(long roleId) {
            return member -> member.getRoles().stream().anyMatch(r -> r.getIdLong() == roleId);
        }
    }

    public static final class Translation {
        private final String translation;
        private final String type;
        private final Language language;

        public Translation(String translation, String type, Language language) {
            this.translation = translation;
            this.type = type;
            this.language = language;
        }

        public String getTranslation() {
            return translation;
        }

        public String getType() {
            return type;
        }

        public Language getLanguage() {
            return language;
        }

        public enum Language {
            GERMAN,
            ENGLISH
        }
    }
}
