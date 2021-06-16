package de.tectoast.emolga.commands;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.reflect.ClassPath;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.*;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.music.SoundSendHandler;
import de.tectoast.emolga.utils.showdown.Analysis;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.SDPokemon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.Helpers;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.bot.EmolgaMain.flegmonjda;

public abstract class Command {
    /**
     * NO PERMISSION Message
     */
    public static final String NOPERM = "Dafür hast du keine Berechtigung!";
    /**
     * File to the main storage of the bot
     */
    public static final File emolgadata = new File("./emolgadata.json");
    /**
     * List of all commands of the bot
     */
    public static final ArrayList<Command> commands = new ArrayList<>();
    /**
     * List of guilds where the chill playlist is playing
     */
    public static final ArrayList<Guild> chill = new ArrayList<>();
    /**
     * List of guilds where the deep playlist is playing
     */
    public static final ArrayList<Guild> deep = new ArrayList<>();
    /**
     * List of guilds where the music playlist is playing
     */
    public static final ArrayList<Guild> music = new ArrayList<>();
    /**
     * Contains all messages which are help messages by the bot
     */
    public static final HashSet<Message> helps = new HashSet<>();
    /**
     * Saves the channel ids per server where emolgas commands work
     */
    public static final HashMap<Long, List<Long>> emolgaChannel = new HashMap<>();
    /**
     * Some pokemon extensions for serebii
     */
    public static final HashMap<String, String> serebiiex = new HashMap<>();
    /**
     * Some pokemon extensions for showdown
     */
    public static final HashMap<String, String> sdex = new HashMap<>();
    /**
     * saves when an user got xp
     */
    public static final HashMap<Long, Long> latestExp = new HashMap<>();
    /**
     * an optional xp multiplicator per user
     */
    public static final HashMap<Long, Double> expmultiplicator = new HashMap<>();
    /**
     * saves per guild a ReplayAnalyser, which does something with the result of a showdown match
     */
    public static final HashMap<Long, ReplayAnalyser> sdAnalyser = new HashMap<>();
    /**
     * saves all channels where emoteSteal is enabled
     */
    public static final ArrayList<Long> emoteSteal = new ArrayList<>();
    /**
     * saves all Muted Roles per guild
     */
    public static final HashMap<Long, Long> mutedRoles = new HashMap<>();
    /**
     * saves all Moderator Roles per guild
     */
    public static final HashMap<Long, Long> moderatorRoles = new HashMap<>();
    /**
     * saves all replay channel with their result channel
     */
    public static final HashMap<Long, Long> replayAnalysis = new HashMap<>();
    /**
     * saves all guilds where spoiler tags should be used in the showdown results
     */
    public static final ArrayList<Long> spoilerTags = new ArrayList<>();
    /**
     * Cache for translations
     */
    public static final HashMap<String, Translation> translationsCache = new HashMap<>();
    /**
     * Order of the cached items, used to delete the oldest after enough caching
     */
    public static final LinkedList<String> translationsCacheOrder = new LinkedList<>();
    /**
     * Mapper for the DraftGerName
     */
    public static final Function<String, String> draftnamemapper = s -> getDraftGerName(s).getTranslation();
    /**
     * Path to the default Showdown Data
     */
    private static final String SDPATH = "./ShowdownData/";
    /**
     * JSONObject where data about pokemon is saved
     */
    public static JSONObject wikijson;
    /**
     * JSONObject where pokemon sugimori sprite links are saved
     */
    public static JSONObject spritejson;
    /**
     * JSONObject of the main storage of the bot
     */
    public static JSONObject emolgajson;
    /**
     * Currently not used
     */
    public static JSONObject huntjson;
    /**
     * Currently not used
     */
    public static JSONObject statisticsjson;
    /**
     * Currently not used
     */
    public static JSONObject ytjson;
    /**
     * JSONObject where the current xp from the users are stored
     */
    public static JSONObject leveljson;
    /**
     * Used for a shiny counter
     */
    public static JSONObject shinycountjson;
    /**
     * JSONObject containing all credentials (Discord Token, Google OAuth Token)
     */
    public static JSONObject tokens;
    /**
     * MusicManagers for Lavaplayer
     */
    public static final HashMap<Long, GuildMusicManager> musicManagers = new HashMap<>();

    public static final HashMap<Long, AudioPlayerManager> playerManagers = new HashMap<>();

    public static HashMap<Long, SoundSendHandler> sendHandlers = new HashMap<>();
    /**
     * True if the XP JSON got edited after the last save
     */
    public static boolean expEdited = false;
    /**
     * Currently not used
     */
    @SuppressWarnings("CanBeFinal")
    public static boolean checkBST = false;
    /**
     * false = Last time it showed how many replay channels there were<br>
     * true = Last time it showed my discord data
     */
    public static boolean lastPresence = false;
    public static HashMap<String, ArrayList<LinkedList<byte[]>>> audioData = new HashMap<>();
    /**
     * Currently not used
     */
    protected static String tradesid;
    /**
     * Currently not used
     */
    protected static List<String> balls;
    /**
     * Currently not used
     */
    protected static List<String> mons;
    protected static long lastClipUsed = -1;
    /**
     * List containing guild ids where this command is enabled, empty if it is enabled in all guilds
     */
    protected final List<Long> allowedGuilds;
    /**
     * Set containing all aliases of this command
     */
    protected final HashSet<String> aliases = new HashSet<>();
    /**
     * HashMap containing a help for a guild id which should be shown for this command on that guild instead of the {@link #help}
     */
    protected final HashMap<Long, String> overrideHelp = new HashMap<>();
    /**
     * HashMap containing a channel list which should be used for this command instead of {@link #emolgaChannel}
     */
    protected final HashMap<Long, List<Long>> overrideChannel = new HashMap<>();
    /**
     * The name of the command, used to check if the command was used in {@link #check(GuildMessageReceivedEvent)}
     */
    protected final String name;
    /**
     * The help string of the command, shown in the help messages by the bot
     */
    protected final String help;
    /**
     * The {@link CommandCategory} which this command is in
     */
    protected final CommandCategory category;
    /**
     * True if this command should use the {@code e!} instead of {@code !}
     */
    protected boolean otherPrefix = false;
    /**
     * The ArgumentManagerTemplate of this command, used for checking arguments
     */
    protected ArgumentManagerTemplate argumentTemplate;
    /**
     * If true, this command is only allowed for me because I'm working on it
     */
    protected boolean wip = false;
    /**
     * True if this command should bypass all channel restrictions
     */
    protected boolean everywhere = false;
    /**
     * Predicate which checks if a member is allowed to use this command, ignored if {@link #customPermissions} is false
     */
    protected Predicate<Member> allowsMember = m -> false;
    /**
     * True if this command should not use the permissions of the CommandCategory but {@link #allowsMember} to test if a user is allowed to use the command
     */
    protected boolean customPermissions = false;
    /**
     * If true, this command is disabled and cannot be used
     */
    protected boolean disabled = false;
    protected long allowedBotId = -1;

    /**
     * Creates a new command and adds is to the list. Each command should use this constructor for one time (see {@link #registerCommands()})
     *
     * @param name     The {@link #name} of the command
     * @param help     The {@link #help} of the command
     * @param category The {@link #category} of the command
     * @param guilds   The {@link #allowedGuilds} of the command, as long array or an empty array if it is allowed everywhere
     */
    public Command(String name, String help, CommandCategory category, long... guilds) {
        this.name = name;
        this.help = help;
        this.category = category;
        allowedGuilds = guilds.length == 0 ? new ArrayList<>() : Arrays.stream(guilds).boxed().collect(Collectors.toCollection(ArrayList::new));
        commands.add(this);
    }

    public static String getFirst(String str) {
        return str.split("-")[0];
    }

    public static String getFirstAfterUppercase(String s) {
        if (!s.contains("-")) return s;
        return s.charAt(0) + s.substring(1, 2).toUpperCase() + s.substring(2);
    }

    public static File invertImage(String mon, boolean shiny) {
        BufferedImage inputFile;
        System.out.println("mon = " + mon);
        try {
            File f = new File("../Showdown/sspclient/sprites/gen5" + (shiny ? "-shiny" : "") + "/" + mon.toLowerCase() + ".png");
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
        Guild g = emolgajda.getGuildById(gid);
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        getPlayerManager(g).loadItemOrdered(musicManager, "https://www.youtube.com/playlist?list=PLrwrdAXSpHC5Mr2zC-q_dWKONVybk6JO6", new AudioLoadResultHandler() {


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
        getPlayerManager(channel.getGuild()).loadItemOrdered(musicManager, track, new AudioLoadResultHandler() {


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
        return commands.stream().filter(c -> c.name.equalsIgnoreCase(name) || c.getAliases().stream().anyMatch(name::equalsIgnoreCase)).findFirst().orElse(null);
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
        if (track.startsWith("https://www.youtube.com/") || track.startsWith("https://youtu.be/")) {
            url = track;
            loader = new YTDataLoader(Google.getVidByURL(url, false));
        } else {
            SearchResult s = Google.getVidByQuery(track, false);
            url = "https://www.youtube.com/watch?v=" + s.getId().getVideoId();
            loader = new YTDataLoader(s);
        }

        System.out.println("url = " + url);
        getPlayerManager(channel.getGuild()).loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
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

    public static void playSound(VoiceChannel vc, String path, TextChannel tc) {
        boolean flegmon = vc.getJDA().getSelfUser().getIdLong() != 723829878755164202L;
        if (System.currentTimeMillis() - lastClipUsed < 10000 && flegmon) {
            tc.sendMessage("Warte bitte noch kurz...").queue();
            return;
        }
        Guild g = vc.getGuild();
        GuildMusicManager gmm = getGuildAudioPlayer(g);
        getPlayerManager(g).loadItemOrdered(gmm, path, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if(flegmon)
                lastClipUsed = System.currentTimeMillis();
                play(g, gmm, track, vc);
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
        /*long gid = g.getIdLong();
        ArrayList<LinkedList<byte[]>> sound = audioData.get(name);
        SoundSendHandler handler;
        if (sendHandlers.containsKey(gid)) {
            handler = sendHandlers.get(gid);
        } else {
            handler = new SoundSendHandler();
            g.getAudioManager().setSendingHandler(handler);
            sendHandlers.put(gid, handler);
        }
        handler.loadSoundBytes(sound.get(new Random().nextInt(sound.size())));*/
    }

    public static boolean removeFromJSONArray(JSONArray arr, Object value) {
        boolean success = false;
        for (int i = 0; i < arr.length(); ) {
            if (arr.get(i).equals(value)) {
                arr.remove(i);
                success = true;
            } else i++;
        }
        return success;
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
        long expires = System.currentTimeMillis() + time * 1000L;
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

    public static void kick(TextChannel tco, Member mod, Member mem, String reason) {
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
        Database.insert("kicks", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
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
        long expires = System.currentTimeMillis() + time * 1000L;
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
            b.append(method).append(": ").append(emolgajda.getGuildById("745934535748747364").getEmoteById(m.getString("emote")).getAsMention()).append("\n");
            for (String s : shinycountjson.getString("userorder").split(",")) {
                b.append(names.getString(s)).append(": ").append(m.optInt(s, 0)).append("\n");
            }
            b.append("\n");
        }
        emolgajda.getTextChannelById("778380440078647296").editMessageById("778380596413464676", b.toString()).queue();
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

        getPlayerManager(vc.getGuild()).loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
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
            musicManager = new GuildMusicManager(getPlayerManager(guild));
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public static synchronized AudioPlayerManager getPlayerManager(Guild guild) {
        long guildId = guild.getIdLong();
        AudioPlayerManager playerManager = playerManagers.get(guildId);

        if (playerManager == null) {
            playerManager = new DefaultAudioPlayerManager();
            playerManagers.put(guildId, playerManager);
        }
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        return playerManager;
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
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append("+");
        }
        return b.toString();
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
        } catch (Exception e) {
            System.out.println("STACKTRACE " + path);
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
        return getModByGuild(e.getGuild());
    }

    public static String getModByGuild(Guild g) {
        String gid = g.getId();
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

    public static JSONObject getSpriteJSON() {
        return spritejson;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void registerCommands() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.commands")) {
                Class<?> cl = classInfo.load();
                if (cl.getSuperclass().getSimpleName().endsWith("Command") && !Modifier.isAbstract(cl.getModifiers())) {
                    //System.out.println(classInfo.getName());
                    cl.getConstructors()[0].newInstance();
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        /*new DataCommand();
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
        new GoinCommand();*/
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
                    //TODO ja Geburtstage halt lmao
                }
            }
        }, c.getTimeInMillis() - System.currentTimeMillis());
    }

    public static LinkedList<byte[]> readAudioData(File f) {
        try {
            LinkedList<byte[]> l = new LinkedList<>();
            FileInputStream fis = new FileInputStream(f);
            while (true) {
                byte[] arr = new byte[3840];
                int x = fis.read(arr);
                l.add(arr);
                if (x == -1) {
                    break;
                }
            }
            fis.close();
            return l;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void init() {
        loadJSONFiles();
        /*File audio = new File("audio/");
        for (File file : audio.listFiles()) {
            System.out.println("Checking file " + file.getName() + "...");
            ArrayList<LinkedList<byte[]>> l = new ArrayList<>();
            if (file.isDirectory()) {
                System.out.println(file.getName() + " is a directory!");
                for (File listFile : file.listFiles()) {
                    System.out.println("found " + listFile.getName() + " within " + file.getName() + "!");
                    l.add(readAudioData(listFile));
                }
            } else {
                System.out.println(file.getName() + " is not a directory, adding it to the list!");
                l.add(readAudioData(file));
            }
            audioData.put(file.getName().split("\\.")[0], l);
        }
        System.out.println("audioData.keySet() = " + audioData.keySet());*/
        new ModManager("default", "./ShowdownData/");
        new ModManager("nml", "../Showdown/sspserver/data/");
        JSONObject mute = emolgajson.getJSONObject("mutedroles");
        for (String s : mute.keySet()) {
            mutedRoles.put(Long.parseLong(s), mute.getLong(s));
        }
        JSONObject mod = emolgajson.getJSONObject("moderatorroles");
        for (String s : mod.keySet()) {
            moderatorRoles.put(Long.parseLong(s), mod.getLong(s));
        }
        JSONObject echannel = emolgajson.getJSONObject("emolgachannel");
        for (String s : echannel.keySet()) {
            emolgaChannel.put(Long.parseLong(s), echannel.getJSONArray(s).toList().stream().map(o -> (Long) o).collect(Collectors.toCollection(ArrayList::new)));
        }
        registerCommands();
        try {
            balls = Files.readAllLines(Paths.get("./balls.txt"));
            mons = Files.readAllLines(Paths.get("./tauschdoc.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        /*emolgaChannel.put(Constants.ASLID, new ArrayList<>(Arrays.asList(728680506098712579L, 736501675447025704L)));
        emolgaChannel.put(Constants.BSID, new ArrayList<>(Arrays.asList(732545253344804914L, 735076688144105493L)));
        emolgaChannel.put(709877545708945438L, new ArrayList<>(Collections.singletonList(738893933462945832L)));
        emolgaChannel.put(677229415629062180L, new ArrayList<>(Collections.singletonList(731455491527540777L)));
        emolgaChannel.put(694256540642705408L, new ArrayList<>(Collections.singletonList(695157832072560651L)));
        emolgaChannel.put(747357029714231299L, new ArrayList<>(Arrays.asList(752802115096674306L, 762411109859852298L)));*/
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
            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 5) + (gameday * 10 + 77), res);
            boolean p1wins = false;
            for (int i = 0; i < 2; i++) {
                String uid = userids.get(i);
                ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                int x = 0;
                int index = table.indexOf(uid);
                List<List<Object>> list = Google.get(sid, "Daten!B" + (index * 15 + 21) + ":C" + (index * 15 + 32), true, false);
                System.out.println("gameday = " + gameday);
                if (str.split(":")[0].equals(uid)) {
                    System.out.println("LINKS Picks von " + game[i].getNickname() + ": " + picks);
                    for (String s : mons[i]) {
                        System.out.println("s = " + s + " ");
                        int monindex = aslIndexPick(picks, s);
                        if (monindex > -1) {
                            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 3) + (gameday * 10 + 78 + x), Arrays.asList(deaths.get(s), kills.get(s), s));
                            List<Object> l = list.get(monindex);
                            b.addRow("Daten!B" + (index * 15 + 21 + monindex),
                                    Arrays.asList(l.get(0) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 4) + (gameday * 10 + 78 + x),
                                            l.get(1) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 3) + (gameday * 10 + 78 + x)));
                            x++;
                        }
                    }
                } else {
                    System.out.println("RECHTS Picks von " + game[i].getNickname() + ": " + picks);
                    for (String s : mons[i]) {
                        System.out.println("s = " + s);
                        int monindex = aslIndexPick(picks, s);
                        if (monindex > -1) {
                            b.addRow("Spielplan!" + getAsXCoord(battleindex * 8 + 7) + (gameday * 10 + 78 + x), Arrays.asList(s, kills.get(s), deaths.get(s)));
                            List<Object> l = list.get(monindex);
                            b.addRow("Daten!B" + (index * 15 + 21 + monindex),
                                    Arrays.asList(l.get(0) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 8) + (gameday * 10 + 78 + x),
                                            l.get(1) + " + Spielplan!" + getAsXCoord(battleindex * 8 + 9) + (gameday * 10 + 78 + x)));
                            x++;
                        }
                    }
                }
                if (game[i].isWinner()) {
                    results.put(uid1 + ":" + uid2, uid);
                    b.addSingle("Daten!I" + (index + 1), (Integer.parseInt((String) Google.get(sid, "Daten!I" + (index + 1), false, false).get(0).get(0)) + 1));
                } else {
                    b.addSingle("Daten!J" + (index + 1), (Integer.parseInt((String) Google.get(sid, "Daten!J" + (index + 1), false, false).get(0).get(0)) + 1));
                }
            }
            b.addSingle("Spielplan!" + getAsXCoord(battleindex * 8 + 6) + (gameday * 10 + 75), "=HYPERLINK(\"" + args[1] + "\"; \"VS\")");
            b.execute();
            saveEmolgaJSON();
            sortASL(league);
            //gameday++;
            //evaluatePredictions(league, p1wins, gameday, uid1, uid2);
        });
        sdAnalyser.put(709877545708945438L, (game, uid1, uid2, kills, deaths, args) -> {
            Guild guild = emolgajda.getGuildById("709877545708945438");
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
        JSONObject gd = predictiongame.getJSONObject("ids").getJSONObject(String.valueOf(gameday + 7));
        String key = gd.has(uid1 + ":" + uid2) ? uid1 + ":" + uid2 : uid2 + ":" + uid1;
        Message message = emolgajda.getTextChannelById(predictiongame.getLong("channelid")).retrieveMessageById(gd.getLong(key)).complete();
        List<User> e1 = message.retrieveReactionUsers(emolgajda.getEmoteById(540970044297838597L)).complete();
        List<User> e2 = message.retrieveReactionUsers(emolgajda.getEmoteById(645622238757781505L)).complete();
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
        return getDataJSON(mod).keySet().stream().filter(s -> !s.endsWith("gmax") && !s.endsWith("totem")).collect(Collectors.toList());
    }

    public static void woolooStyle(String sid, Message message, String uid1, String uid2) {
        Guild guild = emolgajda.getGuildById("709877545708945438");
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
        //jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching(lastPresence ? ("auf " + Database.getData("statistics", "count", "name", "analysis") + " Replays") : ("zu @Flooo#2535")));
        //lastPresence = !lastPresence;
        emolgajda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("auf " + Database.getData("statistics", "count", "name", "analysis") + " Replays"));
    }

    public static String getHelpDescripion(Guild g, Member mem) {
        StringBuilder s = new StringBuilder();
        for (CommandCategory cat : CommandCategory.getOrder()) {
            if (cat.allowsGuild(g) && cat.allowsMember(mem) && getWithCategory(cat, g, mem).size() > 0)
                s.append(cat.isEmote() ? g.getJDA().getEmoteById(cat.emoji).getAsMention() : cat.emoji).append(" ").append(cat.name).append("\n");
        }
        s.append("\u25c0\ufe0f Zurück zur Übersicht");
        return s.toString();
    }

    public static void addReactions(Message m, Member mem) {
        Guild g = m.getGuild();
        for (CommandCategory cat : CommandCategory.getOrder()) {
            if (cat.allowsGuild(g) && cat.allowsMember(mem)) {
                if (cat.isEmote()) m.addReaction(g.getJDA().getEmoteById(cat.emoji)).queue();
                else
                    m.addReaction(cat.emoji).queue();
            }
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
        Bot bot = Bot.byJDA(e.getJDA());

        JSONObject cc = getEmolgaJSON().getJSONObject("customcommands");
        for (String s : cc.keySet()) {
            if (msg.toLowerCase().startsWith("!" + s)) {
                JSONObject o = cc.getJSONObject(s);
                String sendmsg = null;
                File f = null;
                if (!o.get("text").equals(false)) sendmsg = o.getString("text");
                if (!o.get("image").equals(false)) f = new File(o.getString("image"));
                if (sendmsg == null) {
                    tco.sendFile(f).queue();
                } else {
                    MessageAction ac = tco.sendMessage(sendmsg);
                    if (f != null) ac = ac.addFile(f);
                    ac.queue();
                }
            }
        }
        if (bot == Bot.FLEGMON || gid == 745934535748747364L) {
            File dir = new File("audio/clips/");
            for (File file : dir.listFiles()) {
                if (msg.equalsIgnoreCase("!" + file.getName().split("\\.")[0])) {
                    GuildVoiceState voiceState = e.getMember().getVoiceState();
                    if (voiceState.inVoiceChannel()) {
                        AudioManager am = e.getGuild().getAudioManager();
                        if (!am.isConnected()) {
                            am.openAudioConnection(voiceState.getChannel());
                            am.setConnectionListener(new ConnectionListener() {
                                @Override
                                public void onPing(long ping) {

                                }

                                @Override
                                public void onStatusChange(@NotNull ConnectionStatus status) {
                                    System.out.println("status = " + status);
                                    if (status == ConnectionStatus.CONNECTED) {
                                        playSound(voiceState.getChannel(), "/home/florian/Discord/audio/clips/hi.mp3", tco);
                                        playSound(voiceState.getChannel(), file.getPath(), tco);
                                    }
                                }

                                @Override
                                public void onUserSpeaking(@NotNull User user, boolean speaking) {

                                }
                            });

                        } else {
                            playSound(voiceState.getChannel(), file.getPath(), tco);
                        }
                    }
                }
            }
        }
        for (Command command : commands) {
            if (command.disabled) continue;
            if (!command.checkPrefix(msg)) continue;
            if (!command.checkBot(e.getJDA(), gid)) continue;
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
            if (check == PermissionCheck.GUILD_NOT_ALLOWED) break;
            if (check == PermissionCheck.PERMISSION_DENIED) {
                tco.sendMessage(NOPERM).queue();
                break;
            }
            if (!command.everywhere && !command.category.isEverywhere()) {
                if (command.overrideChannel.containsKey(gid)) {
                    List<Long> l = command.overrideChannel.get(gid);
                    if (!l.contains(e.getChannel().getIdLong())) {
                        if(e.getAuthor().getIdLong() == Constants.FLOID) {
                            tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:").queue();
                        } else {
                            e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                            return;
                        }
                    }
                } else {
                    if (emolgaChannel.containsKey(gid)) {
                        List<Long> l = emolgaChannel.get(gid);
                        if (!l.contains(e.getChannel().getIdLong()) && !l.isEmpty()) {
                            if(e.getAuthor().getIdLong() == Constants.FLOID) {
                                tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:").queue();
                            } else {
                                e.getChannel().sendMessage("<#" + l.get(0) + ">").queue();
                                return;
                            }
                        }
                    }
                }
            }
            try {
                Database.incrementStatistic("cmd_" + command.name);
                GuildCommandEvent event = new GuildCommandEvent(command, e);
            } catch (MissingArgumentException ex) {
                ArgumentManagerTemplate.Argument arg = ex.getArgument();
                if (arg.hasCustomErrorMessage()) tco.sendMessage(arg.getCustomErrorMessage()).queue();
                else {
                    tco.sendMessage("Das benötigte Argument `" + arg.getName() + "`, was eigentlich " + buildEnumeration(arg.getTypes()) + " sein müsste, ist nicht vorhanden!\n" +
                            "Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help " + command.getName() + "`.").queue();
                }
                if (mem.getIdLong() != Constants.FLOID) {
                    sendToMe("MissingArgument " + tco.getAsMention());
                }
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
        return mon.getJSONArray("formeOrder").toList().stream().map(o -> toSDName((String) o)).distinct().filter(json::has).map(json::getJSONObject).filter(o -> !o.optString("forme").endsWith("Totem")).collect(Collectors.toList());
        //return json.keySet().stream().filter(s -> s.startsWith(monname.toLowerCase()) && !s.endsWith("gmax") && (s.equalsIgnoreCase(monname) || json.getJSONObject(s).has("forme"))).sorted(Comparator.comparingInt(String::length)).map(json::getJSONObject).collect(Collectors.toList());
    }

    public static ArrayList<String> getAttacksFrom(String pokemon, String msg, String form, String mod) {
        return getAttacksFrom(pokemon, msg, form, 8, mod);
    }

    public static boolean moveFilter(String msg, String move) {
        JSONObject o = getEmolgaJSON().getJSONObject("movefilter");
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

    public static void sendToMe(String msg, Bot... bot) {
        sendToUser(Constants.FLOID, msg, bot);
    }

    public static void sendDexEntry(String msg) {
        emolgajda.getTextChannelById(839540004908957707L).sendMessage(msg).queue();
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

    public static void sendToUser(long id, String msg, Bot... bot) {
        JDA jda;
        if (bot.length == 0) jda = emolgajda;
        else jda = bot[0].getJDA();
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
            emolgajda.getGuildById(Constants.BSID).retrieveMembersByIds(players.toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
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

    public static void analyseASLReplay(long authorid, String url, Message m) {
        long gid = Constants.ASLID;
        JDA jda = m.getJDA();
        Guild g = jda.getGuildById(gid);
        System.out.println(url);
        Player[] game;
        try {
            game = Analysis.analyse(url, m);
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
            str = name1 + " ||" + winloose + "|| " + name2 + "\n\n" + name1 + ":\n" + t1
                    + "\n" + name2 + ": " + "\n" + t2;
        } else {
            str = name1 + " " + winloose + " " + name2 + "\n\n" + name1 + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1
                    + "\n" + name2 + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2;
        }
        g.getTextChannelById(league.getLong("replaychannel")).sendMessage("Spieltag " + (gid == Constants.ASLID ? gameday + 7 : gameday) + "\n" + name1 + " vs " + name2 + "\n" + url).queue();
        TextChannel resultchannel = g.getTextChannelById(league.getLong("resultchannel"));
        resultchannel.sendMessage(str).queue();
        Database.incrementStatistic("analysis");
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

    /*public static void sendEarlyASLReplays(int gameday) {
        JSONObject early = getEmolgaJSON().getJSONObject("earlyreplays");
        if (!early.has(String.valueOf(gameday))) return;
        for (Object o : early.getJSONArray(String.valueOf(gameday))) {
            analyseASLReplay(Constants.FLOID, (String) o, emolgajda);
        }
        early.remove(String.valueOf(gameday));
        saveEmolgaJSON();
    }*/

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
        int after = Integer.parseInt(f.format(new Date(currmillis + delayinmins * 60000L)));
        int now = Integer.parseInt(f.format(new Date(currmillis)));
        if ((now >= from && now < to) && (after < from || after >= to))
            return ((to - from) * 60L + delayinmins) * 60000;
        else if (now >= to || now < from) {
            Calendar c = Calendar.getInstance();
            if (now >= to)
                c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, from + (delayinmins / 60));
            c.set(Calendar.MINUTE, (delayinmins % 60));
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis() - System.currentTimeMillis();
        } else return delayinmins * 60000L;
    }

    public static long getXPNeeded(int level) {
        return (long) (5 * (Math.pow(level, 2)) + 50L * level + 100);
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

    public static void sortMainASL(JSONObject league) {
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

    public static void sortASL(JSONObject league) {
        sortMainASL(league);
        String sid = league.getString("sid");
        RequestBuilder b = new RequestBuilder(sid);
        for (int sc = 0; sc < 2; sc++) {
            List<List<Object>> formula = Google.get(sid, "Tabelle!B" + (sc * 5 + 14) + ":J" + (sc * 5 + 17), true, false);
            List<List<Object>> points = Google.get(sid, "Tabelle!C" + (sc * 5 + 14) + ":J" + (sc * 5 + 17), false, false);
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
            for (int j = 0; j < 4; j++) {
                senddata.add(valmap.get(j));
            }
            b.addAll("Tabelle!B" + (sc * 5 + 14), senddata);
        }
        b.execute();
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

    public static Translation getBSTGerName(String s) {
        Translation check = checkShortcuts(s);
        if (check.isSuccess()) return check;
        System.out.println("BSTGerName s = " + s);
        Translation ret;
        if (s.equalsIgnoreCase("wulaosu") || s.equalsIgnoreCase("urshifu")) {
            return new Translation("ONLYWITHFORM", Translation.Type.UNKNOWN, Translation.Language.UNKNOWN);
        }
        if (serebiiex.keySet().stream().anyMatch(s::equalsIgnoreCase)) {
            String[] ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining("")).split("-");
            System.out.println("ex = " + Arrays.toString(ex));
            ret = getGerName(ex[0]).append("-" + ex[1]);
            System.out.println("ret = " + ret);
            return ret;
        }
        if (s.toLowerCase().startsWith("a-")) {
            Translation gername = getGerName(s.substring(2));
            if (!gername.isFromType(Translation.Type.POKEMON)) return Translation.empty();
            ret = gername.before("A-");
            System.out.println("ret = " + ret);
            return ret;
        } else if (s.toLowerCase().startsWith("g-")) {
            Translation gername = getGerName(s.substring(2));
            if (!gername.isFromType(Translation.Type.POKEMON)) return Translation.empty();
            ret = gername.before("G-");
            System.out.println("ret = " + ret);
            return ret;
        }
        ret = getGerName(s);
        System.out.println("ret = " + ret);
        return ret;
    }

    public static Translation getDraftGerName(String s) {
        System.out.println("getDraftGerName s = " + s);
        if (getGerName(s).isSuccess()) return getGerName(s);
        String[] split = s.split("-");
        System.out.println("getDraftGerName Arr = " + Arrays.toString(split));
        if (s.toLowerCase().startsWith("m-")) {
            String sub = s.substring(2);
            Translation mon;
            if (s.endsWith("-X")) mon = getGerName(sub.substring(0, sub.length() - 2)).append("-X");
            else if (s.endsWith("-Y")) mon = getGerName(sub.substring(0, sub.length() - 2)).append("-Y");
            else mon = getGerName(sub);
            if (!mon.isFromType(Translation.Type.POKEMON)) return Translation.empty();
            return mon.before("M-");
        } else if (s.toLowerCase().startsWith("a-")) {
            Translation mon = getGerName(s.substring(2));
            if (!mon.isFromType(Translation.Type.POKEMON)) return Translation.empty();
            return mon.before("A-");
        } else if (s.toLowerCase().startsWith("g-")) {
            Translation mon = getGerName(s.substring(2));
            if (!mon.isFromType(Translation.Type.POKEMON)) return Translation.empty();
            return mon.before("G-");
        }
        System.out.println("split[0] = " + split[0]);
        Translation t = getGerName(split[0]);
        System.out.print("DraftGer Trans ");
        t.print();
        if (t.isSuccess()) {
            t.append("-" + split[1]);
            System.out.println("getDraftGerName ret = " + t);
            return t;
        }
        return Translation.empty();
    }

    public static Translation getGerName(String s) {
        return getGerName(s, "default");
    }

    public static Translation getGerName(String s, String mod) {
        if (translationsCache.containsKey(toSDName(s))) {
            return translationsCache.get(toSDName(s));
        }
        Translation check = checkShortcuts(s);
        if (check.isSuccess()) return check;
        System.out.println("getGerName s = " + s);
        /*if (serebiiex.keySet().stream().anyMatch(s::equalsIgnoreCase)) {
            String[] ex = serebiiex.keySet().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining("")).split("-");
            return new Translation(getGerName(ex[0]).getTranslation() + ex[1], Translation.Type.POKEMON, Translation.Language.GERMAN);
        }*/
        ResultSet set = getTranslation(s, mod);
        try {
            if (set.next()) {
                Translation t = new Translation(set.getString("germanname"), Translation.Type.fromId(set.getString("type")), Translation.Language.GERMAN, set.getString("englishname"));
                addToCache(s, t);
                return t;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Translation.empty();
    }

    public static String getGerNameNoCheck(String s) {
        if (translationsCache.containsKey(toSDName(s))) {
            return translationsCache.get(toSDName(s)).getTranslation();
        }
        System.out.println("NoCheckGerName s = " + s);
        ResultSet set = getTranslation(s, null);
        try {
            set.next();
            Translation t = new Translation(set.getString("germanname"), Translation.Type.fromId(set.getString("type")), Translation.Language.GERMAN, set.getString("englishname"));
            addToCache(s, t);
            return t.getTranslation();
        } catch (SQLException throwables) {
            return null;
        }
    }

    public static void addToCache(String name, Translation t) {
        translationsCache.put(toSDName(name), t.copy());
        translationsCacheOrder.add(toSDName(name));
        if (translationsCacheOrder.size() > 1000) {
            translationsCache.remove(translationsCacheOrder.removeFirst());
        }
    }

    public static String getEnglName(String s) {
        return getEnglName(s, "default");
    }

    public static String getEnglName(String s, String mod) {
        Translation str = getEnglNameWithType(s, mod);
        if (str.isEmpty()) return "";
        return str.getTranslation();
    }

    public static Translation getEnglNameWithType(String s) {
        return getEnglNameWithType(s, "default");
    }

    public static Translation getEnglNameWithType(String s, String mod) {
        Translation check = checkShortcuts(s);
        if (check.isSuccess()) {
            return getEnglNameWithType(check.getTranslation());
        }
        ResultSet set = getTranslation(s, mod);
        try {
            if (set.next()) {
                return new Translation(set.getString("englishname"), Translation.Type.fromId(set.getString("type")), Translation.Language.ENGLISH, set.getString("germanname"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Translation.empty();
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
        Translation engl = getEnglNameWithType(s, mod);
        if (engl.isEmpty()) return "";
        return toSDName(engl.getTranslation());
    }

    public static String toSDName(String s) {
        return s.toLowerCase().replaceAll("[^a-zA-Z0-9äöüÄÖÜß♂♀]+", "");
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

    public static Translation checkShortcuts(String s) {
        return Translation.fromOldString(getEmolgaJSON().getJSONObject("shortcuts").optString(s.toLowerCase(), null));
    }

    public static String getMonName(String s, long gid) {
        return getMonName(s, gid, true);
    }

    public static String getMonName(String s, long gid, boolean withDebug) {
        if (withDebug)
            System.out.println("s = " + s);
        if (gid == 709877545708945438L) {
            if (s.endsWith("-Alola")) {
                return "Alola-" + getGerName(s.substring(0, s.length() - 6)).getTranslation();
            } else if (s.endsWith("-Galar")) {
                return "Galar-" + getGerName(s.substring(0, s.length() - 6)).getTranslation();
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
        if (s.equals("Deoxys-Attack")) return "Deoxys-Atk";
        if (s.equals("Deoxys-Speed")) return "Deoxys-Speed";
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
            else return "Amigento-" + getGerName(split[1]).getTranslation();
        }
        if (s.contains("Arceus")) {
            String[] split = s.split("-");
            if (split.length == 1 || s.equals("Arceus-*")) return "Arceus";
            else if (split[1].equals("Psychic")) return "Arceus-Psycho";
            else return "Arceus-" + getGerName(split[1]).getTranslation();
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
            return "M-" + getGerName(s.substring(0, s.length() - 5)).getTranslation();
        } else if (s.endsWith("-Alola")) {
            return "A-" + getGerName(s.substring(0, s.length() - 6)).getTranslation();
        } else if (s.endsWith("-Galar")) {
            return "G-" + getGerName(s.substring(0, s.length() - 6)).getTranslation();
        } else if (s.endsWith("-Therian")) {
            return getGerName(s.substring(0, s.length() - 8)).getTranslation() + "-T";
        } else if (s.endsWith("-X")) {
            return "M-" + getGerName(s.split("-")[0]).getTranslation() + "-X";
        } else if (s.endsWith("-Y")) {
            return "M-" + getGerName(s.split("-")[0]).getTranslation() + "-Y";
        }
        if (s.equals("Tornadus")) return "Boreos-I";
        if (s.equals("Thundurus")) return "Voltolos-I";
        if (s.equals("Landorus")) return "Demeteros-I";
        Translation gername = getGerName(s);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(s.split("-")));
        if (gername.isFromType(Translation.Type.POKEMON)) {
            return gername.getTranslation();
        }
        String first = split.remove(0);
        return getGerName(first).getTranslation() + "-" + String.join("-" + split);
    }

    public static String buildEnumeration(ArgumentType... types) {
        return buildEnumeration(Arrays.stream(types).map(ArgumentType::getName).toArray(String[]::new));
    }

    public static String buildEnumeration(String... types) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                if (i + 1 == types.length)
                    builder.append(" oder ");
                else
                    builder.append(", ");
            }
            builder.append(types[i]);
        }
        return builder.toString();
    }

    public static boolean isChannelAllowed(TextChannel tc) {
        long gid = tc.getGuild().getIdLong();
        return (!emolgaChannel.containsKey(gid) || emolgaChannel.get(gid).contains(tc.getIdLong()) || emolgaChannel.get(gid).isEmpty());
    }

    protected void setCustomPermissions(Predicate<Member> predicate) {
        this.allowsMember = predicate.or(member -> member.getIdLong() == Constants.FLOID);
        this.customPermissions = true;
    }

    protected void disable() {
        this.disabled = true;
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
        if (!allowsGuild(gid)) return PermissionCheck.GUILD_NOT_ALLOWED;
        if (!allowsMember(mem)) return PermissionCheck.PERMISSION_DENIED;
        return PermissionCheck.GRANTED;
    }

    private boolean checkPrefix(String msg) {
        if (category == CommandCategory.Music || otherPrefix) {
            return msg.toLowerCase().startsWith("e!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("e!" + s.toLowerCase() + " "))
                    || msg.equalsIgnoreCase("e!" + name) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("e!" + s));
        }
        return msg.toLowerCase().startsWith("!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("!" + s.toLowerCase() + " "))
                || msg.equalsIgnoreCase("!" + name.toLowerCase()) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("!" + s));
    }

    private boolean checkBot(JDA jda, long guildid) {
        return allowedBotId == -1 || allowedBotId == jda.getSelfUser().getIdLong() || guildid == Constants.CULT;
    }

    public String getPrefix() {
        return otherPrefix ? "e!" : "!";
    }

    /**
     * Abstract method, which is called on the subclass with the corresponding command when the command was received
     *
     * @param e A GuildCommandEvent containing the informations about the command
     * @throws Exception Every exception that can be thrown in any command
     */
    public abstract void process(GuildCommandEvent e) throws Exception;

    public String getName() {
        return name;
    }

    public String getHelp() {
        return help;
    }

    public HashSet<String> getAliases() {
        return aliases;
    }

    public ArgumentManagerTemplate getArgumentTemplate() {
        return argumentTemplate;
    }

    public void setArgumentTemplate(ArgumentManagerTemplate template) {
        this.argumentTemplate = template;
    }

    public String getHelp(Guild g) {
        ArgumentManagerTemplate args = getArgumentTemplate();
        return ("`" + (args.hasSyntax() ? args.getSyntax() : getPrefix() + getName() + (args.arguments.size() > 0 ? " " : "")
                + args.arguments.stream().map(a -> (a.isOptional() ? "[" : "<") + a.getName() + (a.isOptional() ? "]" : ">")).collect(Collectors.joining(" ")))) + "` " + overrideHelp.getOrDefault(g.getIdLong(), help) + (wip ? " (**W.I.P.**)" : "");
    }

    public String getHelpWithoutCmd(Guild g) {
        return overrideHelp.getOrDefault(g.getIdLong(), help) + (wip ? " (**W.I.P.**)" : "");
    }

    public enum Bot {
        EMOLGA(emolgajda),
        FLEGMON(flegmonjda);

        final JDA jda;

        Bot(JDA jda) {
            this.jda = jda;
        }

        public static Bot byJDA(JDA jda) {
            for (Bot value : values()) {
                if (jda.getSelfUser().getIdLong() == value.getJDA().getSelfUser().getIdLong()) {
                    return value;
                }
            }
            return null;
        }

        public JDA getJDA() {
            return jda;
        }
    }

    private enum PermissionCheck {
        GRANTED,
        PERMISSION_DENIED,
        GUILD_NOT_ALLOWED
    }

    public interface ArgumentType {
        Object validate(String str, Object... params);

        String getName();

        String getCustomHelp();
    }

    public static final class PermissionPreset {
        public static final Predicate<Member> OWNER = Member::isOwner;
        public static final Predicate<Member> ADMIN = member -> member.hasPermission(Permission.ADMINISTRATOR);
        public static final Predicate<Member> MODERATOR = m -> ADMIN.test(m) || m.getRoles().stream().anyMatch(r -> Command.moderatorRoles.containsValue(r.getIdLong()));
        public static final Predicate<Member> CULT = fromRole(781457314846343208L);

        public static Predicate<Member> fromRole(long roleId) {
            return member -> member.getRoles().stream().anyMatch(r -> r.getIdLong() == roleId);
        }

        public static Predicate<Member> fromIDs(long... ids) {
            return member -> Arrays.stream(ids).anyMatch(l -> member.getIdLong() == l);
        }
    }

    public static class ArgumentException extends Exception {
        final ArgumentManagerTemplate.Argument argument;

        public ArgumentException(ArgumentManagerTemplate.Argument argument) {
            this.argument = argument;
        }

        public ArgumentManagerTemplate.Argument getArgument() {
            return argument;
        }
    }

    public static final class MissingArgumentException extends ArgumentException {

        public MissingArgumentException(ArgumentManagerTemplate.Argument argument) {
            super(argument);
        }
    }

    public static final class ArgumentManager {
        private final HashMap<String, Object> map;

        public ArgumentManager(HashMap<String, Object> map) {
            this.map = map;
        }

        public Member getMember(String key) {
            return (Member) map.get(key);
        }

        public Translation getTranslation(String key) {
            return (Translation) map.get(key);
        }

        public String getText(String key) {
            return (String) map.get(key);
        }

        public boolean isText(String key, String text) {
            return map.getOrDefault(key, "").equals(text);
        }

        public boolean has(String key) {
            return map.containsKey(key);
        }

        public <T> T getOrDefault(String key, T defvalue) {
            return (T) map.getOrDefault(key, defvalue);
        }

        public long getID(String key) {
            return (long) map.get(key);
        }

        public Role getRole(String key) {
            return (Role) map.get(key);
        }

        public <C> boolean is(String key, Class<? extends C> cl) {
            return cl.isInstance(map.get(key));
        }

        public int getInt(String key) {
            return (int) map.get(key);
        }

        public TextChannel getChannel(String key) {
            return (TextChannel) map.get(key);
        }
    }

    public static final class SubCommand {
        final String name;
        final String help;

        private SubCommand(String name, String help) {
            this.name = name;
            this.help = help;
        }

        public static SubCommand of(String name, String help) {
            return new SubCommand(name, help);
        }

        public static SubCommand of(String name) {
            return new SubCommand(name, null);
        }

        public String getName() {
            return name;
        }

        public String getHelp() {
            return help == null ? "" : " " + help;
        }
    }

    public static final class ArgumentManagerTemplate {

        private static final ArgumentManagerTemplate noCheckTemplate;

        static {
            noCheckTemplate = new ArgumentManagerTemplate();
        }

        private final boolean noCheck;
        public LinkedList<Argument> arguments;
        private String example;
        private String syntax;

        private ArgumentManagerTemplate(LinkedList<Argument> arguments, boolean noCheck, String example, String syntax) {
            this.arguments = arguments;
            this.noCheck = noCheck;
            this.example = example;
            this.syntax = syntax;
        }

        private ArgumentManagerTemplate() {
            noCheck = true;
            this.arguments = new LinkedList<>();
        }

        public static Builder builder() {
            return new Builder();
        }

        @SuppressWarnings("SameReturnValue")
        public static ArgumentManagerTemplate noArgs() {
            return noCheckTemplate;
        }

        public static ArgumentManagerTemplate noSpecifiedArgs(String syntax, String example) {
            return new ArgumentManagerTemplate(new LinkedList<>(), true, example, syntax);
        }

        public static ArgumentType draft() {
            return withPredicate("Draftname", getEmolgaJSON().getJSONObject("drafts")::has, false);
        }

        public static ArgumentType withPredicate(String name, Predicate<String> check, boolean female) {
            return new ArgumentType() {
                @Override
                public Object validate(String str, Object... params) {
                    return check.test(str) ? str : null;
                }

                @Override
                public String getName() {
                    return "ein" + (female ? "e" : "") + " **" + name + "**";
                }

                @Override
                public String getCustomHelp() {
                    return null;
                }
            };
        }

        public static ArgumentType withPredicate(String name, Predicate<String> check, boolean female, Function<String, String> mapper) {
            return new ArgumentType() {
                @Override
                public Object validate(String str, Object... params) {
                    if (check.test(str)) {
                        System.out.println("Before map: " + str);
                        String applied = mapper.apply(str);
                        System.out.println("After Map:" + applied);
                        return applied;
                    }
                    return null;
                }

                @Override
                public String getName() {
                    return "ein" + (female ? "e" : "") + " **" + name + "**";
                }

                @Override
                public String getCustomHelp() {
                    return null;
                }
            };
        }

        public String getExample() {
            return example;
        }

        public boolean hasExample() {
            return example != null;
        }

        public String getSyntax() {
            return syntax;
        }

        public boolean hasSyntax() {
            return syntax != null;
        }

        public ArgumentManager construct(GuildMessageReceivedEvent e) throws ArgumentException {
            if (noCheck) return null;
            Message m = e.getMessage();
            String raw = m.getContentRaw();
            ArrayList<String> split = new ArrayList<>(Arrays.asList(raw.split("\\s+")));
            split.remove(0);
            HashMap<DiscordType, Integer> asFar = new HashMap<>();
            HashMap<String, Object> map = new HashMap<>();
            int argumentI = 0;
            for (int i = 0; i < split.size(); ) {
                if (i >= arguments.size()) {
                    break;
                }
                if (argumentI >= arguments.size()) break;
                Argument a = arguments.get(argumentI);
                String str = argumentI + 1 == arguments.size() ? String.join(" ", split.subList(i, split.size())) : split.get(i);
                ArgumentType[] types = a.getTypes();
                Object o;
                int count = 1;
                for (ArgumentType type : types) {
                    if (type instanceof DiscordType) {
                        o = type.validate(str, m, asFar.getOrDefault(type, 0));
                        if (o != null) asFar.put((DiscordType) type, asFar.getOrDefault(type, 0) + 1);
                    } else {
                        o = type.validate(str, a.getLanguage(), getModByGuild(e.getGuild()));
                    }
                    if (o == null) {
                        if (!a.isOptional() && count == types.length) throw new MissingArgumentException(a);
                    } else {
                        map.put(a.getId(), o);
                        i++;
                        break;
                    }
                    count++;
                }
                argumentI++;
            }
            if (arguments.stream().anyMatch(argument -> !argument.optional) && map.size() == 0)
                throw new MissingArgumentException(arguments.stream().filter(argument -> !argument.optional).findFirst().orElse(null));
            return new ArgumentManager(map);
        }


        public enum DiscordType implements ArgumentType {
            USER(Pattern.compile("<@!*\\d{18,22}>"), "User", false),
            CHANNEL(Pattern.compile("<#*\\d{18,22}>"), "Channel", false),
            ROLE(Pattern.compile("<@&*\\d{18,22}>"), "Rolle", true),
            ID(Pattern.compile("\\d{18,22}"), "ID", true),
            INTEGER(Pattern.compile("\\d{1,9}"), "Zahl", true);

            final Pattern pattern;
            final String name;
            final boolean female;

            DiscordType(Pattern pattern, String name, boolean female) {
                this.pattern = pattern;
                this.name = name;
                this.female = female;
            }

            public Pattern getPattern() {
                return pattern;
            }

            @Override
            public Object validate(String str, Object... params) {
                if (pattern.matcher(str).find()) {
                    if (mentionable()) {
                        Message m = (Message) params[0];
                        int soFar = (int) params[1];
                        return this == USER ? m.getMentionedMembers().get(soFar) : this == CHANNEL ? m.getMentionedChannels().get(soFar) : m.getMentionedRoles().get(soFar);
                    }
                    if (this == ID) return Long.parseLong(str);
                    if (this == INTEGER) return Integer.parseInt(str);
                }
                return null;
            }

            public boolean mentionable() {
                return this == USER || this == CHANNEL || this == ROLE;
            }

            @Override
            public String getName() {
                return "ein" + (female ? "e" : "") + " **" + name + "**";
            }

            @Override
            public String getCustomHelp() {
                return null;
            }


        }

        public static final class Text implements ArgumentType {

            private final LinkedList<SubCommand> texts = new LinkedList<>();
            private final boolean any;
            private final boolean withOf;
            private Function<String, String> mapper = s -> s;

            private Text(SubCommand[] possible) {
                texts.addAll(Arrays.asList(possible));
                any = false;
                withOf = true;
            }

            private Text() {
                any = true;
                withOf = false;
            }

            public static Text of(SubCommand... possible) {
                return new Text(possible);
            }

            public static Text any() {
                return new Text();
            }

            public Text setMapper(Function<String, String> mapper) {
                this.mapper = mapper;
                return this;
            }

            @Override
            public Object validate(String str, Object... params) {
                if (any) return str;
                return texts.stream().map(SubCommand::getName).filter(scName -> scName.equalsIgnoreCase(str)).map(mapper).findFirst().orElse(null);
            }

            @Override
            public String getName() {
                return "ein Text";
            }

            @Override
            public String getCustomHelp() {
                return texts.stream().map(sc -> "`" + sc.getName() + "`" + sc.getHelp()).collect(Collectors.joining("\n"));
            }

        }

        public static final class Number implements ArgumentType {

            private final LinkedList<Integer> numbers = new LinkedList<>();
            private final boolean any;
            private final boolean withOf;
            private int from;
            private int to;
            private boolean hasRange;

            private Number(Integer[] possible) {
                numbers.addAll(Arrays.asList(possible));
                any = false;
                withOf = true;
                hasRange = false;
            }

            private Number() {
                any = true;
                withOf = false;
                hasRange = false;
            }

            public static Number range(int from, int to) {
                LinkedList<Integer> l = new LinkedList<>();
                for (int i = from; i <= to; i++) {
                    l.add(i);
                }
                return of(l.toArray(new Integer[0])).setRange(from, to);
            }

            public static Number of(Integer... possible) {
                return new Number(possible);
            }

            public static Number any() {
                return new Number();
            }

            private Number setRange(int from, int to) {
                this.from = from;
                this.to = to;
                this.hasRange = true;
                return this;
            }

            public int getFrom() {
                return from;
            }

            public int getTo() {
                return to;
            }

            public boolean hasRange() {
                return hasRange;
            }

            @Override
            public Object validate(String str, Object... params) {
                if (Helpers.isNumeric(str)) {
                    int num = Integer.parseInt(str);
                    if (any) return num;
                    return numbers.contains(num) ? num : null;
                }
                return null;
            }

            @Override
            public String getName() {
                return "ein Text";
            }

            @Override
            public String getCustomHelp() {
                if (hasRange()) {
                    return from + "-" + to;
                }
                return numbers.stream().map(String::valueOf).collect(Collectors.joining(","));
            }
        }

        public static final class Argument {
            private final String id;
            private final String name;
            private final String help;
            private final ArgumentType[] type;
            private final boolean optional;
            private final Translation.Language language;
            private final String customErrorMessage;

            public Argument(String id, String name, String help, ArgumentType type, boolean optional, Translation.Language language, String customErrorMessage) {
                this.id = id;
                this.name = name;
                this.optional = optional;
                this.type = new ArgumentType[]{type};
                this.help = help;
                this.language = language;
                this.customErrorMessage = customErrorMessage;
            }

            public Argument(String id, String name, String help, ArgumentType[] type, boolean optional, Translation.Language language, String customErrorMessage) {
                this.id = id;
                this.name = name;
                this.optional = optional;
                this.type = type;
                this.help = help;
                this.language = language;
                this.customErrorMessage = customErrorMessage;
            }

            public String getHelp() {
                StringBuilder b = new StringBuilder(help);
                if (type[0].getCustomHelp() == null) return b.toString();
                ArgumentType t = type[0];
                if (!help.equals("")) {
                    b.append("\n");
                }
                return b.append("Möglichkeiten:\n").append(t.getCustomHelp()).toString();
            }

            public String getId() {
                return id;
            }

            public ArgumentType[] getTypes() {
                return type;
            }

            public boolean isOptional() {
                return optional;
            }

            public Translation.Language getLanguage() {
                return language;
            }

            public String getName() {
                return name;
            }

            public String getCustomErrorMessage() {
                return customErrorMessage;
            }

            public boolean hasCustomErrorMessage() {
                return customErrorMessage != null;
            }
        }

        public static final class Builder {

            private final LinkedList<Argument> arguments = new LinkedList<>();
            private boolean noCheck = false;
            private String example;
            private String customDescription;

            public Builder add(String id, String name, String help, ArgumentType type) {
                return add(id, name, help, type, false);
            }

            public Builder add(String id, String name, String help, ArgumentType type, boolean optional) {
                return add(id, name, help, type, optional, null);
            }

            public Builder add(String id, String name, String help, ArgumentType type, boolean optional, String customErrorMessage) {
                arguments.add(new Argument(id, name, help, type, optional, Translation.Language.GERMAN, customErrorMessage));
                return this;
            }

            public Builder addMultiple(String id, String name, String help, boolean optional, ArgumentType... type) {
                return addMultiple(id, name, help, optional, null, type);
            }

            public Builder addMultiple(String id, String name, String help, boolean optional, String customErrorMessage, ArgumentType... type) {
                arguments.add(new Argument(id, name, help, type, optional, Translation.Language.GERMAN, customErrorMessage));
                return this;
            }

            public Builder addEngl(String id, String name, String help, ArgumentType type) {
                return addEngl(id, name, help, type, false);
            }

            public Builder addEngl(String id, String name, String help, ArgumentType type, boolean optional) {
                return addEngl(id, name, help, type, optional, null);
            }

            public Builder addEngl(String id, String name, String help, ArgumentType type, boolean optional, String customErrorMessage) {
                arguments.add(new Argument(id, name, help, type, optional, Translation.Language.ENGLISH, customErrorMessage));
                return this;
            }

            public Builder setNoCheck(boolean noCheck) {
                this.noCheck = noCheck;
                return this;
            }

            public Builder setExample(String example) {
                this.example = example;
                return this;
            }

            public Builder setCustomDescription(String customDescription) {
                this.customDescription = customDescription;
                return this;
            }

            public ArgumentManagerTemplate build() {
                return new ArgumentManagerTemplate(arguments, noCheck, example, customDescription);
            }
        }
    }

    public static final class Translation {
        private static final Translation emptyTranslation;

        static {
            emptyTranslation = new Translation("", Type.UNKNOWN, Language.UNKNOWN);
            emptyTranslation.empty = true;
        }

        private final Type type;
        private final Language language;
        private String translation;
        private boolean empty;
        private String otherLang = "";

        public Translation(String translation, Type type, Language language) {
            this.translation = translation;
            this.type = type;
            this.language = language;
            this.empty = false;
        }

        public Translation(String translation, Type type, Language language, String otherLang) {
            this.translation = translation;
            this.type = type;
            this.language = language;
            this.otherLang = otherLang;
            this.empty = false;
        }

        public static Translation empty() {
            return emptyTranslation;
        }

        public static Translation fromOldString(String str) {
            if (str == null) return empty();
            String[] arr = str.split(";");
            return new Translation(arr[1], Type.fromId(arr[0]), Language.GERMAN, getEnglName(arr[1]));
        }

        public Translation copy() {
            if (isEmpty()) return empty();
            return new Translation(translation, type, language, otherLang);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Translation{");
            sb.append("type=").append(type);
            sb.append(", language=").append(language);
            sb.append(", translation='").append(translation).append('\'');
            sb.append(", empty=").append(empty);
            sb.append(", otherLang='").append(otherLang).append('\'');
            sb.append('}');
            emolgajda.getTextChannelById(837027867417641021L).sendMessage(sb.toString()).queue();
            return sb.toString();
        }

        public void print() {
            String sb = "Translation{" + "type=" + type +
                    ", language=" + language +
                    ", translation='" + translation + '\'' +
                    ", empty=" + empty +
                    ", otherLang='" + otherLang + '\'' +
                    '}';
            System.out.println(sb);
        }

        public String getOtherLang() {
            return otherLang;
        }

        public Translation append(String str) {
            translation += str;
            return this;
        }

        public Translation before(String str) {
            translation = str + translation;
            return this;
        }

        public Translation after(String str) {
            return append(str);
        }

        public boolean isFromType(Type type) {
            return this.type == type;
        }

        public String getTranslation() {
            return translation;
        }

        public Type getType() {
            return type;
        }

        public Language getLanguage() {
            return language;
        }

        public boolean isSuccess() {
            return !empty;
        }

        public boolean isEmpty() {
            return empty;
        }

        public String toOldString() {
            return type.getId() + ";" + translation;
        }

        public enum Language {
            GERMAN,
            ENGLISH,
            UNKNOWN
        }

        public enum Type implements ArgumentType {
            ABILITY("abi", "Fähigkeit", true),
            EGGGROUP("egg", "Eigruppe", true),
            ITEM("item", "Item", false),
            MOVE("atk", "Attacke", true),
            NATURE("nat", "Wesen", false),
            POKEMON("pkmn", "Pokémon", false),
            TYPE("type", "Typ", false),
            UNKNOWN("unknown", "Undefiniert", false);

            final String id;
            final String name;
            final boolean female;

            Type(String id, String name, boolean female) {
                this.id = id;
                this.name = name;
                this.female = female;
            }

            public static Type fromId(String id) {
                return Arrays.stream(values()).filter(t -> t.getId().equalsIgnoreCase(id)).findFirst().orElse(UNKNOWN);
            }

            public static ArgumentType of(Type... types) {
                return new ArgumentType() {
                    @Override
                    public Object validate(String str, Object... params) {
                        Translation t = params[0] == Language.GERMAN ? getGerName(str) : getEnglNameWithType(str);
                        if (t.isEmpty()) return null;
                        if (!Arrays.asList(types).contains(t.getType())) return null;
                        return t;
                    }

                    @Override
                    public String getName() {
                        //return "ein **Pokémon**, eine **Attacke**, ein **Item** oder eine **Fähigkeit**";
                        return buildEnumeration(types);
                    }

                    @Override
                    public String getCustomHelp() {
                        return null;
                    }
                };
            }

            public static ArgumentType all() {
                return of(Arrays.stream(values()).filter(t -> t != UNKNOWN).toArray(Type[]::new));
            }

            public String getId() {
                return id;
            }

            @Override
            public Object validate(String str, Object... params) {
                String mod = (String) params[1];
                Translation t = params[0] == Language.GERMAN ? getGerName(str, mod) : getEnglNameWithType(str, mod);
                if (t.isEmpty()) return null;
                if (t.getTranslation().equals("Psychic") || t.getOtherLang().equals("Psychic")) {
                    if (this == TYPE) {
                        return new Translation(t.getLanguage() == Language.GERMAN ? "Psycho" : "Psychic", TYPE, t.getLanguage());
                    }
                }
                if (t.getType() != this) return null;
                return t;
            }

            @Override
            public String getName() {
                return "ein" + (female ? "e" : "") + " **" + name + "**";
            }

            @Override
            public String getCustomHelp() {
                return null;
            }
        }
    }
}
