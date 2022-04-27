package de.tectoast.emolga.commands;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.common.reflect.ClassPath;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.buttons.ButtonListener;
import de.tectoast.emolga.buttons.buttonsaves.MonData;
import de.tectoast.emolga.buttons.buttonsaves.Nominate;
import de.tectoast.emolga.buttons.buttonsaves.TrainerData;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.selectmenus.MenuListener;
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet;
import de.tectoast.emolga.utils.*;
import de.tectoast.emolga.utils.annotations.ToTest;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.music.SoundSendHandler;
import de.tectoast.emolga.utils.records.CalendarEntry;
import de.tectoast.emolga.utils.records.DeferredSlashResponse;
import de.tectoast.emolga.utils.records.TimerData;
import de.tectoast.emolga.utils.showdown.Analysis;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.Pokemon;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import de.tectoast.jsolf.JSONTokener;
import de.tectoast.toastilities.repeat.RepeatTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.Helpers;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.bot.EmolgaMain.flegmonjda;
import static de.tectoast.emolga.utils.Constants.*;
import static de.tectoast.emolga.utils.draft.Draft.doMatchUps;
import static de.tectoast.emolga.utils.draft.Draft.doNDSNominate;
import static java.util.Calendar.*;
import static net.dv8tion.jda.api.entities.UserSnowflake.fromId;

public abstract class Command {
    /**
     * NO PERMISSION Message
     */
    public static final String NOPERM = "Dafür hast du keine Berechtigung!";
    public static final List<Long> NML_GUILDS = Collections.singletonList(786351922029527130L);
    /**
     * List of all commands of the bot
     */
    public static final Map<String, Command> commands = new HashMap<>();
    /**
     * List of guilds where the chill playlist is playing
     */
    public static final List<Guild> chill = new ArrayList<>();
    /**
     * List of guilds where the deep playlist is playing
     */
    public static final List<Guild> deep = new ArrayList<>();
    /**
     * List of guilds where the music playlist is playing
     */
    public static final List<Guild> music = new ArrayList<>();
    /**
     * Contains all messages which are help messages by the bot
     */
    public static final Set<Message> helps = new HashSet<>();
    /**
     * Saves the channel ids per server where emolgas commands work
     */
    public static final Map<Long, List<Long>> emolgaChannel = new HashMap<>();
    /**
     * Some pokemon extensions for serebii
     */
    public static final Map<String, String> serebiiex = new HashMap<>();
    /**
     * Some pokemon extensions for showdown
     */
    public static final Map<String, String> sdex = new HashMap<>();
    /**
     * saves when an user got xp
     */
    public static final Map<Long, Long> latestExp = new HashMap<>();
    /**
     * an optional xp multiplicator per user
     */
    public static final Map<Long, Double> expmultiplicator = new HashMap<>();
    /**
     * saves per guild a ReplayAnalyser, which does something with the result of a showdown match
     */
    public static final Map<Long, ReplayAnalyser> sdAnalyser = new HashMap<>();
    /**
     * saves all channels where emoteSteal is enabled
     */
    public static final List<Long> emoteSteal = new ArrayList<>();
    /**
     * saves all Muted Roles per guild
     */
    public static final Map<Long, Long> mutedRoles = new HashMap<>();
    /**
     * saves all Moderator Roles per guild
     */
    public static final Map<Long, Long> moderatorRoles = new HashMap<>();
    /**
     * saves all replay channel with their result channel
     */
    public static final Map<Long, Long> replayAnalysis = new HashMap<>();
    /**
     * saves all guilds where spoiler tags should be used in the showdown results
     */
    public static final List<Long> spoilerTags = new ArrayList<>();
    /**
     * Cache for german translations
     */
    public static final Map<String, Translation> translationsCacheGerman = new HashMap<>();
    /**
     * Order of the cached items, used to delete the oldest after enough caching
     */
    public static final LinkedList<String> translationsCacheOrderGerman = new LinkedList<>();
    /**
     * Cache for english translations
     */
    public static final Map<String, Translation> translationsCacheEnglish = new HashMap<>();
    /**
     * Order of the cached items, used to delete the oldest after enough caching
     */
    public static final LinkedList<String> translationsCacheOrderEnglish = new LinkedList<>();
    /**
     * MusicManagers for Lavaplayer
     */
    public static final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    public static final Map<Long, AudioPlayerManager> playerManagers = new HashMap<>();
    public static final Map<Long, TrainerData> trainerDataButtons = new HashMap<>();
    public static final Map<Long, MonData> monDataButtons = new HashMap<>();
    public static final Map<Long, Nominate> nominateButtons = new HashMap<>();
    public static final Map<Long, SmogonSet> smogonMenu = new HashMap<>();
    public static final List<Long> customResult = Collections.singletonList(NDSID);
    public static final Map<Long, CircularFifoQueue<byte[]>> clips = new HashMap<>();
    protected static final Map<Long, String> soullinkIds = Map.of(448542640850599947L, "Pascal", 726495601021157426L, "David", 867869302808248341L, "Jesse", 541214204926099477L, "Felix");
    protected static final List<String> soullinkNames = Arrays.asList("Pascal", "David", "Jesse", "Felix");
    private static final Logger logger = LoggerFactory.getLogger(Command.class);
    /**
     * Mapper for the DraftGerName
     */
    public static final Function<String, String> draftnamemapper = s -> getDraftGerName(s).getTranslation();
    /**
     * Path to the default Showdown Data
     */
    private static final String SDPATH = "./ShowdownData/";
    private static final JSONObject typeIcons = load("typeicons.json");
    /**
     * JSONObject where pokemon sugimori sprite links are saved
     */
    public static JSONObject spritejson;
    /**
     * JSONObject of the main storage of the bot
     */
    public static JSONObject emolgajson;
    /**
     * Used for a shiny counter
     */
    public static JSONObject shinycountjson;
    /**
     * JSONObject containing all credentials (Discord Token, Google OAuth Token)
     */
    public static JSONObject tokens;
    public static JSONObject catchrates;
    public static Map<Long, SoundSendHandler> sendHandlers = new HashMap<>();
    public static AtomicInteger replayCount = new AtomicInteger();
    protected static long lastClipUsed = -1;
    protected static ScheduledExecutorService calendarService = Executors.newScheduledThreadPool(5);
    protected static ScheduledExecutorService moderationService = Executors.newScheduledThreadPool(5);
    protected static ScheduledExecutorService birthdayService = Executors.newScheduledThreadPool(1);
    /**
     * List containing guild ids where this command is enabled, empty if it is enabled in all guilds
     */
    protected final List<Long> allowedGuilds;
    /**
     * Set containing all aliases of this command
     */
    protected final Set<String> aliases = new HashSet<>();
    /**
     * HashMap containing a help for a guild id which should be shown for this command on that guild instead of the {@link #help}
     */
    protected final Map<Long, String> overrideHelp = new HashMap<>();
    /**
     * HashMap containing a channel list which should be used for this command instead of {@link #emolgaChannel}
     */
    protected final Map<Long, List<Long>> overrideChannel = new HashMap<>();
    /**
     * The name of the command, used to check if the command was used in {@link #check(MessageReceivedEvent)}
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
     * If true, sends a beta information when using this command
     */
    protected boolean beta = false;
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
    protected boolean slash = false;
    protected boolean onlySlash = false;

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
        logger.info("mon = " + mon);
        try {
            File f = new File("../Showdown/sspclient/sprites/gen5" + (shiny ? "-shiny" : "") + "/" + mon.toLowerCase() + ".png");
            logger.info("f.getAbsolutePath() = " + f.getAbsolutePath());
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

    public static void loadPlaylist(final TextChannel channel, final String track, Member mem, String cm) {
        loadPlaylist(channel, track, mem, cm, false);
    }

    public static void loadPlaylist(final TextChannel channel, final String track, Member mem, String cm, boolean random) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        logger.info(track);
        getPlayerManager(channel.getGuild()).loadItemOrdered(musicManager, track, new AudioLoadResultHandler() {


            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                LinkedList<AudioTrack> toplay = new LinkedList<>();
                if (random) {
                    ArrayList<AudioTrack> list = new ArrayList<>(playlist.getTracks());
                    Collections.shuffle(list);
                    toplay.addAll(list);
                } else {
                    toplay.addAll(playlist.getTracks());
                }
                for (AudioTrack playlistTrack : toplay) {
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
        return commands.values().stream().filter(c -> c.name.equalsIgnoreCase(name) || c.getAliases().stream().anyMatch(name::equalsIgnoreCase)).findFirst().orElse(null);
    }

    public static void loadAndPlay(final TextChannel channel, final String track, Member mem, String cm) throws IllegalArgumentException {
        /*if (track.startsWith("https://www.youtube.com/playlist")) {
            loadPlaylist(channel, track, mem, cm);
            return;
        }*/
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        logger.info(track);
        YTDataLoader loader = YTDataLoader.create(track);
        String url = loader.getUrl();
        logger.info("url = " + url);
        getPlayerManager(channel.getGuild()).loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                //logger.info("LOADED!");
                if (cm == null) {
                    channel.sendMessageEmbeds(loader.buildEmbed(track, mem, musicManager)).queue();
                } else {
                    channel.sendMessage(cm).queue();
                }
                play(channel.getGuild(), musicManager, track, mem, channel);
            }


            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (cm == null) {
                    channel.sendMessageEmbeds(loader.buildEmbed(playlist, mem, musicManager)).queue();
                } else {
                    channel.sendMessage(cm).queue();
                }
                for (AudioTrack playlistTrack : playlist.getTracks()) {
                    play(channel.getGuild(), musicManager, playlistTrack, mem, channel);
                }
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

    public static void playSound(AudioChannel vc, String path, TextChannel tc) {
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
                if (flegmon)
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
        int minutes = (int) (timeseconds / 60);
        if (minutes > 0) {
            builder.append("**").append(minutes).append("** ").append(pluralise(minutes, "Minute", "Minuten")).append(", ");
            timeseconds = timeseconds % 60;
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

    public static int getGameDayByTeamnames(JSONObject league, String team1, String team2) {
        List<String> teams = league.getStringList("teams");
        return getGameDay(league, String.valueOf(league.getJSONObject("S1").getLongList("table").get(teams.indexOf(team1))), String.valueOf(league.getJSONObject("S2").getLongList("table").get(teams.indexOf(team2))));
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
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        if (mutedRoles.containsKey(g.getIdLong()))
            g.addRoleToMember(mem, g.getRoleById(mutedRoles.get(g.getIdLong()))).queue();
        long expires = (System.currentTimeMillis() + time * 1000L) / 1000 * 1000;
        muteTimer(g, expires, mem.getIdLong());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        //Database.insert("mutes", "userid, modid, guildid, reason, expires", mem.getIdLong(), mod.getIdLong(), g.getIdLong(), reason, new Timestamp(expires));
        DBManagers.MUTE.mute(mem.getIdLong(), mod.getIdLong(), g.getIdLong(), reason, new Timestamp(expires));
    }

    public static void muteTimer(Guild g, long expires, long mem) {
        if (expires == -1) return;
        moderationService.schedule(() -> {
            long gid = g.getIdLong();
            if (DBManagers.MUTE.unmute(mem, gid) != 0) {
                g.removeRoleFromMember(fromId(mem), g.getRoleById(mutedRoles.get(gid))).queue();
            }
        }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static void kick(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht kicken!");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        if (mem.getIdLong() == FLOID) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Das lässt du lieber bleiben :3");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        g.kick(mem, reason).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gekickt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        //Database.insert("kicks", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
        DBManagers.KICKS.kick(mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
    }

    public static void ban(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht bannen!");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        if (mem.getIdLong() == FLOID) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Das lässt du lieber bleiben :3");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        g.ban(mem, 0, reason).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gebannt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        DBManagers.BAN.ban(mem.getIdLong(), mem.getUser().getName(), mod.getIdLong(), g.getIdLong(), reason, null);
    }

    public static void mute(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        long gid = g.getIdLong();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht muten!");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        if (mutedRoles.containsKey(gid)) {
            g.addRoleToMember(mem, g.getRoleById(mutedRoles.get(gid))).queue();
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde gemutet", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        DBManagers.MUTE.mute(mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason, null);
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
        tco.sendMessageEmbeds(builder.build()).queue();
        Database.update("delete from mutes where guildid=" + tco.getGuild().getId() + " and userid=" + mem.getId());
    }

    public static void tempBan(TextChannel tco, Member mod, Member mem, int time, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht bannen!");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        g.ban(mem, 0, reason).queue();
        long expires = System.currentTimeMillis() + time * 1000L;
        banTimer(g, expires, mem.getIdLong());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde für " + secondsToTime(time).replace("*", "") + " gebannt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        DBManagers.BAN.ban(mem.getIdLong(), mem.getUser().getName(), mod.getIdLong(), tco.getGuild().getIdLong(), reason, new Timestamp(expires));
    }

    public static void banTimer(Guild g, long expires, long mem) {
        if (expires == -1) return;
        moderationService.schedule(() -> {
            long gid = g.getIdLong();
            if (DBManagers.BAN.unban(mem, gid) != 0) {
                g.unban(fromId(mem)).queue();
            }
        }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static void warn(TextChannel tco, Member mod, Member mem, String reason) {
        Guild g = tco.getGuild();
        if (!g.getSelfMember().canInteract(mem)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("Ich kann diesen User nicht warnen!");
            tco.sendMessageEmbeds(builder.build()).queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(mem.getEffectiveName() + " wurde verwarnt", null, mem.getUser().getEffectiveAvatarUrl());
        builder.setColor(Color.CYAN);
        builder.setDescription("**Grund:** " + reason);
        tco.sendMessageEmbeds(builder.build()).queue();
        //Database.insert("warns", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
        DBManagers.WARNS.warn(mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
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

    public static String getNumber(Map<String, String> map, String pick) {
        //logger.info(map);
        for (String s : map.keySet()) {
            if (pick.contains("Amigento") && s.contains("Amigento") || s.equals(pick) || pick.equals("M-" + s) || pick.contains("Wulaosu") && s.contains("Wulaosu"))
                return map.get(s);
        }
        return "";
    }

    public static int indexPick(List<String> picks, String mon) {
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

    public static List<RowData> getCellsAsRowData(CellData object, int x, int y) {
        List<RowData> list = new LinkedList<>();
        for (int i = 0; i < y; i++) {
            list.add(new RowData().setValues(getXTimes(object, x)));
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

    protected static String buildCalendar() {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM. HH:mm");
        String str = DBManagers.CALENDAR.getAllEntries().stream().sorted(Comparator.comparing(o -> o.expires().getTime())).map(
                o -> "**%s:** %s".formatted(f.format(o.expires()), o.message())
        ).collect(Collectors.joining("\n"));
        return str.isEmpty() ? "_leer_" : str;
    }

    protected static void scheduleCalendarEntry(long expires, String message) {
        calendarService.schedule(() -> {
            TextChannel calendarTc = emolgajda.getTextChannelById(CALENDAR_TCID);
            DBManagers.CALENDAR.delete(new Timestamp(expires / 1000 * 1000));
            calendarTc.sendMessage("(<@%d>) %s".formatted(FLOID, message)).setActionRow(Button.primary("calendar;delete", "Löschen")).queue();
            calendarTc.editMessageById(CALENDAR_MSGID, buildCalendar()).queue();
        }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static void scheduleCalendarEntry(CalendarEntry e) {
        scheduleCalendarEntry(e.expires().getTime(), e.message());
    }

    protected static long parseCalendarTime(String str) throws NumberFormatException {
        String timestr = str.toLowerCase();
        if (!timestr.matches("\\d{1,8}[smhdw]?")) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(SECOND, 0);
            boolean hoursSet = false;
            for (String s : str.split(";")) {
                String[] split = s.split("[.|:]");
                if (s.contains(".")) {
                    calendar.set(DAY_OF_MONTH, Integer.parseInt(split[0]));
                    calendar.set(MONTH, Integer.parseInt(split[1]) - 1);
                } else if (s.contains(":")) {
                    calendar.set(HOUR_OF_DAY, Integer.parseInt(split[0]));
                    calendar.set(MINUTE, Integer.parseInt(split[1]));
                    hoursSet = true;
                }
            }
            if (!hoursSet) {
                calendar.set(HOUR_OF_DAY, 15);
                calendar.set(MINUTE, 0);
            }
            return calendar.getTimeInMillis();
        }
        int multiplier = 1000;
        switch (timestr.charAt(timestr.length() - 1)) {
            case 'w':
                multiplier *= 7;
            case 'd':
                multiplier *= 24;
            case 'h':
                multiplier *= 60;
            case 'm':
                multiplier *= 60;
            case 's':
                timestr = timestr.substring(0, timestr.length() - 1);
        }
        return System.currentTimeMillis() + (long) multiplier * Integer.parseInt(timestr);
    }

    public static void updateShinyCounts(long id) {
        emolgajda.getTextChannelById(id).editMessageById(id == 778380440078647296L ? 778380596413464676L : 925446888772239440L, buildAndSaveShinyCounts()).queue();
    }

    public static void updateSoullink() {
        emolgajda.getTextChannelById(SOULLINK_TCID).editMessageById(SOULLINK_MSGID, buildSoullink()).queue();
    }

    private static Supplier<Stream<String>> soullinkCols() {
        return () -> Stream.concat(soullinkNames.stream(), Stream.of("Fundort", "Status"));
    }

    public static String buildTable(List<List<String>> list) {
        List<Integer> colsizes = IntStream.range(0, list.get(0).size()).mapToObj(i -> list.stream().mapToInt(l -> l.get(i).length()).max().orElse(0)).toList();
        return colsizes.toString();
    }

    public static String buildSoullink() {
        List<String> statusOrder = Arrays.asList("Team", "Box", "RIP");
        JSONObject soullink = emolgajson.getJSONObject("soullink");
        JSONObject mons = soullink.getJSONObject("mons");
        List<String> order = soullink.getStringList("order");
        int maxlen = Math.max(order.stream().mapToInt(String::length).max().orElse(0), Math.max(mons.keySet().stream().map(mons::getJSONObject).flatMap(o -> o.keySet().stream().map(o::getString).toList().stream())
                .map(String::length).max(Comparator.naturalOrder()).orElse(-1), 7)) + 1;
        StringBuilder b = new StringBuilder("```");
        soullinkCols().get().map(s -> ew(s, maxlen)).forEach(b::append);
        b.append("\n");
        for (String s : order.stream().sorted(Comparator.comparing(str -> statusOrder.indexOf(mons.getJSONObject(str).getString("status")))).toList()) {
            JSONObject o = mons.getJSONObject(s);
            String status = o.getString("status");
            b.append(soullinkCols().get().map(n -> ew(switch (n) {
                case "Fundort" -> s;
                case "Status" -> status;
                default -> o.optString(n, "");
            }, maxlen)).collect(Collectors.joining(""))).append("\n");
        }
        return b.append("```").toString();
    }

    /**
     * Expand whitespaces
     *
     * @param str the string
     * @param len the length
     * @return the whitespaced string
     */
    public static String ew(String str, int len) {
        return str + " ".repeat(Math.max(0, len - str.length()));
    }

    public static void updateShinyCounts(ButtonInteractionEvent e) {
        e.editMessage(buildAndSaveShinyCounts()).queue();
    }

    public static String buildAndSaveShinyCounts() {
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
        save(shinycountjson, "shinycount.json");
        return b.toString();
    }

    public static void sortWoolooS4(String sid, int leagu, JSONObject league) {
        String range = "Tabelle!%s%d:%s%d".formatted(getAsXCoord(leagu * 8 - 5), 22, getAsXCoord(leagu * 8), 27);
        List<List<Object>> formula = Google.get(sid, range, true, false);
        List<List<Object>> points = Google.get(sid, range, false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        List<List<Long>> ll = league.getLongListList("table" + leagu);
        points.sort((o1, o2) -> {
            int c = compareColumns(o1, o2, 5);
            if (c != 0) return c;
            long u1 = ll.get((Integer.parseInt(String.valueOf(formula.get(points.indexOf(o1)).get(0)).substring(21)) - 2) / 15).get(0);
            long u2 = ll.get((Integer.parseInt(String.valueOf(formula.get(points.indexOf(o2)).get(0)).substring(21)) - 2) / 15).get(0);
            boolean b = u1 == 785079235421143040L || u2 == 785079235421143040L;
            if (b) {
                logger.info("u1 = {}", u1);
                logger.info("u2 = {}", u2);
            }
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ":" + u2)) {
                    int i = o.getLong(u1 + ":" + u2) == u1 ? -1 : 1;
                    if (b) logger.warn(String.valueOf(i));
                    return i;
                }
                if (o.has(u2 + ":" + u1)) {
                    int i = o.getLong(u2 + ":" + u1) == u1 ? -1 : 1;
                    if (b) logger.warn(String.valueOf(i));
                    return i;
                }
            }
            return compareColumns(o1, o2, 4, 2);
        });
        Collections.reverse(points);
        //logger.info(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 6; j++) {
            sendname.add(namap.get(j));
        }
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, range.substring(0, range.length() - 4), sendname, false);
        logger.info("Done!");
    }

    public static void sortASLS10(String sid, JSONObject league) {
        sortOneASLS10(sid, league, 1);
        sortOneASLS10(sid, league, 2);
    }

    public static void sortOneASLS10(String sid, JSONObject league, int num) {
        String range = "Tabelle!C%d:J%d".formatted(num * 8 + 8, num * 8 + 11);
        List<List<Object>> formula = Google.get(sid, range, true, false);
        List<List<Object>> points = Google.get(sid, range, false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        List<Long> table = league.getLongList("table");
        points.sort((o1, o2) -> {
            int c = compareColumns(o1, o2, 7);
            if (c != 0) return c;
            long u1 = table.get((Integer.parseInt(String.valueOf(formula.get(points.indexOf(o1)).get(0)).substring("='Teamseite RR'!D".length())) - 2) / 15);
            long u2 = table.get((Integer.parseInt(String.valueOf(formula.get(points.indexOf(o2)).get(0)).substring("='Teamseite RR'!D".length())) - 2) / 15);
            logger.info("u1 = {}", u1);
            logger.info("u2 = {}", u2);
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ":" + u2)) {
                    int ret = o.getLong(u1 + ":" + u2) == u1 ? 1 : -1;
                    logger.info("u1u2 ret = {}", ret);
                    return ret;
                }
                if (o.has(u2 + ":" + u1)) {
                    int ret = o.getLong(u2 + ":" + u1) == u1 ? 1 : -1;
                    logger.info("u2u1 ret = {}", ret);
                    return ret;
                }
            }
            return compareColumns(o1, o2, 6, 4);
        });
        Collections.reverse(points);
        //logger.info(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < points.size(); j++) {
            sendname.add(namap.get(j));
        }
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, range.substring(0, range.length() - 4), sendname);
        logger.info("Done!");
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
        //logger.info(points);
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
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, tablename + "!F14", sendname, false);
        logger.info("Done!");
    }

    public static void sortFPL(String sid, String tablename, JSONObject league) {
        List<List<Object>> formula = Google.get(sid, tablename + "!B2:H9", true, false);
        List<List<Object>> points = Google.get(sid, tablename + "!B2:H9", false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        JSONObject docnames = getEmolgaJSON().getJSONObject("docnames");
        points.sort((o1, o2) -> {
            return compareColumns(o1, o2, 1, 4, 2);
            /*String u1 = docnames.getString((String) o1.get(0));
            String u2 = docnames.getString((String) o2.get(0));
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ";" + u2)) return o.getString(u1 + ";" + u2).equals(u1) ? 1 : -1;
                if (o.has(u2 + ";" + u1)) return o.getString(u2 + ";" + u1).equals(u1) ? 1 : -1;
            }*/
            //return 0;
        });
        Collections.reverse(points);
        //logger.info(points);
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
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, tablename + "!B2", sendname, false);
        logger.info("Done!");
    }

    public static void sortBADS(String sid, JSONObject league) {
        List<List<Object>> formula = Google.get(sid, "Tabelle!B2:J11", true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle!C2:J11", false, false);
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
        //logger.info(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            sendname.add(namap.get(j));
        }
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, "Tabelle!B2", sendname);
        logger.info("Done!");
    }

    public static void sortNDS(String sid, JSONObject league) {
        sortNDSSingle(sid, league, 0);
        sortNDSSingle(sid, league, 1);
    }

    public static String reverseGet(JSONObject o, Object value) {
        logger.info("value = {}", value);
        for (String s : o.keySet()) {
            Object obj = o.get(s);
            if (obj.equals(value)) return s;
        }
        return null;
    }

    public static void sortNDSSingle(String sid, JSONObject league, int num) {
        int start = num * 8 + 3;
        int end = num * 8 + 8;
        List<List<Object>> formula = Google.get(sid, "Tabelle RR!B" + start + ":K" + end, true, false);
        List<List<Object>> points = Google.get(sid, "Tabelle RR!C" + start + ":K" + end, false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        JSONObject docnames = getEmolgaJSON().getJSONObject("docnames");
        JSONObject teamnames = league.getJSONObject("teamnames");
        points.sort((o1, o2) -> {
            int c = compareColumns(o1, o2, 1, 7, 5);
            if (c != 0) return c;
            String u1 = reverseGet(teamnames, String.valueOf(formula.get(points.indexOf(o1)).get(0)).split("'")[1]);
            String u2 = reverseGet(teamnames, String.valueOf(formula.get(points.indexOf(o2)).get(0)).split("'")[1]);
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ";" + u2)) return o.getString(u1 + ";" + u2).equals(u1) ? 1 : -1;
                if (o.has(u2 + ";" + u1)) return o.getString(u2 + ";" + u1).equals(u1) ? 1 : -1;
            }
            return 0;
        });
        Collections.reverse(points);
        //logger.info(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < namap.size(); j++) {
            sendname.add(namap.get(j));
        }
        logger.info(String.valueOf(sendname));
        RequestBuilder.updateAll(sid, "Tabelle RR!B" + start, sendname);
        logger.info("Done!");
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
        //logger.info(points);
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
            if (mem.getVoiceState().inAudioChannel()) {
                audioManager.openAudioConnection(mem.getVoiceState().getChannel());
            } else {
                tc.sendMessage("Du musst dich in einem Voicechannel befinden!").queue();
            }
        }
        musicManager.scheduler.queue(track);
    }

    public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        AudioManager audioManager = guild.getAudioManager();
        Member mem = guild.retrieveMemberById(FLOID).complete();
        if (!audioManager.isConnected()) {
            if (mem.getVoiceState().inAudioChannel()) {
                audioManager.openAudioConnection(mem.getVoiceState().getChannel());
            } else {
                sendToMe("Du musst dich in einem Voicechannel befinden!");
            }
        }
        musicManager.scheduler.queue(track);
    }

    public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, AudioChannel vc) {
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
    }

    public static String stars(int n) {
        return "+".repeat(Math.max(0, n));
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
            logger.info("path = " + path);
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
            logger.info("STACKTRACE " + path);
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getTypeIcons() {
        return typeIcons;
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
        return NML_GUILDS.contains(g.getIdLong()) ? "nml" : "default";
    }

    public static JSONObject getEmolgaJSON() {
        return emolgajson;
    }

    public static synchronized void saveEmolgaJSON() {
        save(emolgajson, "emolgadata.json");
    }

    public static void save(JSONObject json, String filename) {
        try {
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

    private static void registerCommands() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.commands")) {
                Class<?> cl = classInfo.load();
                if (cl.isInterface()) continue;
                String name = cl.getSuperclass().getSimpleName();
                if (name.endsWith("Command") && !Modifier.isAbstract(cl.getModifiers())) {
                    Object o = cl.getConstructors()[0].newInstance();
                    if (o instanceof Command)
                        ((Command) o).addToMap();
                    if (cl.isAnnotationPresent(ToTest.class)) {
                        logger.warn("{} has to be tested!", cl.getName());
                    }
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void awaitNextDay() {
        Calendar c = Calendar.getInstance();
        c.add(DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        long tilnextday = c.getTimeInMillis() - System.currentTimeMillis() + 1000;
        logger.info("System.currentTimeMillis() = " + System.currentTimeMillis());
        logger.info("tilnextday = " + tilnextday);
        logger.info("System.currentTimeMillis() + tilnextday = " + (System.currentTimeMillis() + tilnextday));
        logger.info("SELECT REQUEST: " + "SELECT * FROM birthdays WHERE month = " + (c.get(Calendar.MONTH) + 1) + " AND day = " + c.get(DAY_OF_MONTH));
        birthdayService.schedule(() -> {
            ResultSet set = Database.select("SELECT * FROM birthdays WHERE month = " + (c.get(Calendar.MONTH) + 1) + " AND day = " + c.get(DAY_OF_MONTH));
            try {
                TextChannel tc = flegmonjda.getTextChannelById(605650587329232896L);
                while (set.next()) {
                    tc.sendMessage("Alles Gute zum " + (Calendar.getInstance().get(Calendar.YEAR) - set.getInt("year")) + ". Geburtstag, <@" + set.getLong("userid") + ">!").queue();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            awaitNextDay();
        }, tilnextday, TimeUnit.MILLISECONDS);
    }

    public static Collection<ActionRow> getTrainerDataActionRow(TrainerData dt, boolean withMoveset) {
        return Arrays.asList(
                ActionRow.of(SelectMenu.create("trainerdata").addOptions(
                        dt.getMonsList().stream().map(s -> SelectOption.of(s, s).withDefault(dt.isCurrent(s))).collect(Collectors.toList())
                ).build()),
                ActionRow.of(withMoveset ? Button.success("trainerdata;CHANGEMODE", "Mit Moveset") : Button.secondary("trainerdata;CHANGEMODE", "Ohne Moveset"))
        );
    }

    public static void setupRepeatTasks() {
        new RepeatTask(Instant.ofEpochMilli(1651010400000L), 5, Duration.ofDays(7L), day -> doNDSNominate(), true);
        new RepeatTask(Instant.ofEpochMilli(1650823200000L), 5, Duration.ofDays(7L), day -> doMatchUps(String.valueOf(day)), true);
        new RepeatTask(Instant.ofEpochMilli(1651442400000L), 3, Duration.ofDays(7L), day -> {
            for (long tcid : Arrays.asList(934881485121523862L, 934881531971895297L, 934881637727096922L, 934881660854489098L, 934881557334867978L,
                    934881655531925554L, 934881603086327828L, 934881645268439061L, 934881662020497508L, 934881747731120169L)) {
                emolgajda.getTextChannelById(tcid).sendMessage("_**--- Spieltag %d ---**_".formatted(day + 7)).queue();
            }
        }, true);
    }

    public static void init() {
        loadJSONFiles();
        new ModManager("default", "./ShowdownData/");
        new ModManager("nml", "../Showdown/sspserver/data/");
        new Thread(() -> {
            ButtonListener.init();
            MenuListener.init();
            registerCommands();
            setupRepeatTasks();
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
            sdex.put("Burmadame-Pflz", "");
            sdex.put("Burmadame-Pflanze", "");
            sdex.put("Burmadame-Sand", "-sandy");
            sdex.put("Burmadame-Boden", "-sandy");
            sdex.put("Burmadame-Lumpen", "-trash");
            sdex.put("Burmadame-Stahl", "-trash");
            sdex.put("Boreos-T", "-therian");
            sdex.put("Demeteros-T", "-therian");
            sdex.put("Deoxys-Def", "-defense");
            sdex.put("Deoxys-Speed", "-speed");
            sdex.put("Hoopa-U", "-unbound");
            sdex.put("Wulaosu-Wasser", "-rapidstrike");
            sdex.put("Demeteros-I", "");
            sdex.put("Rotom-Heat", "-heat");
            sdex.put("Rotom-Wash", "-wash");
            sdex.put("Rotom-Mow", "-mow");
            sdex.put("Rotom-Fan", "-fan");
            sdex.put("Rotom-Frost", "-frost");
            sdex.put("Wolwerock-Zw", "-dusk");
            sdex.put("Wolwerock-Dusk", "-dusk");
            sdex.put("Wolwerock-Tag", "");
            sdex.put("Wolwerock-Nacht", "-midnight");
            sdex.put("Boreos-I", "");
            sdex.put("Voltolos-T", "-therian");
            sdex.put("Voltolos-I", "");
            sdex.put("Zygarde-50%", "");
            sdex.put("Zygarde-10%", "-10");
            sdex.put("Psiaugon-W", "f");
            sdex.put("Psiaugon-M", "");
            sdex.put("Nidoran-M", "m");
            sdex.put("Nidoran-F", "f");
        /*emolgaChannel.put(Constants.ASLID, new ArrayList<>(Arrays.asList(728680506098712579L, 736501675447025704L)));
        emolgaChannel.put(Constants.BSID, new ArrayList<>(Arrays.asList(732545253344804914L, 735076688144105493L)));
        emolgaChannel.put(709877545708945438L, new ArrayList<>(Collections.singletonList(738893933462945832L)));
        emolgaChannel.put(677229415629062180L, new ArrayList<>(Collections.singletonList(731455491527540777L)));
        emolgaChannel.put(694256540642705408L, new ArrayList<>(Collections.singletonList(695157832072560651L)));
        emolgaChannel.put(747357029714231299L, new ArrayList<>(Arrays.asList(752802115096674306L, 762411109859852298L)));*/
            sdAnalyser.put(ASLID, (game, uid1, uid2, kills, deaths, args) -> {
                JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");

                JSONObject league = null;
                for (int i = 1; i <= 5; i++) {
                    JSONObject l = drafts.getJSONObject("ASLS10L" + i);
                    List<Long> table = l.getLongList("table");
                    if (table.contains(Long.parseLong(uid1)) && table.contains(Long.parseLong(uid2))) {
                        league = l;
                        break;
                    }
                }
                if (league == null) return;
                String sid = league.getString("sid");
                int gameday = getGameDay(league, uid1, uid2);
                if (gameday == -1) {
                    logger.error("GAMEDAY -1");
                    return;
                }
                int gdi = gameday - 1 - 7;
                List<String> users = Arrays.asList(uid1, uid2);
                int i = 0;
                List<Long> table = league.getLongList("table");
                RequestBuilder b = new RequestBuilder(sid);
                for (String uid : users) {
                    int index = table.indexOf(Long.parseLong(uid));
                    List<String> picks = getSortedListOfMons(league.getJSONObject("picks").getJSONList(uid));
                    List<List<Object>> list = new ArrayList<>();
                    int x = 0;
                    for (String pick : picks) {
                        String kill = getNumber(kills.get(i), pick);
                        String death = getNumber(deaths.get(i), pick);
                        list.add(Arrays.asList(
                                death.equals("") ? "-" : 1,
                                kill.equals("") ? "-" : Integer.parseInt(kill),
                                death.equals("") ? "-" : Integer.parseInt(death)
                        ));
                        x++;
                    }
                    if (game[i].isWinner()) {
                        b.addSingle("Stats!C%d".formatted(index * 12 + 2 + gameday), "1");
                        if (!league.has("results"))
                            league.put("results", new JSONObject());
                        league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                    } else {
                        b.addSingle("Stats!D%d".formatted(index * 12 + 2 + gameday), "1");
                    }
                    try {
                        b.addAll("Teamseite RR!%s%d".formatted(getAsXCoord(gdi * 3 + 11), index * 15 + 4), list);
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                    i++;
                }
                generateResult(b, game, league, gameday, uid1, "Spielplan", "ASLS10", (String) args[1]);
                JSONObject finalLeague = league;
                b.withRunnable(() -> sortASLS10(sid, finalLeague), 3000).execute();
                saveEmolgaJSON();
            });
            sdAnalyser.put(FLPID, (game, uid1, uid2, kills, deaths, args) -> {
                JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("WoolooCupS4");
                String sid = league.getString("sid");
                int gameday = getGameDay(league, uid1, uid2);
                if (gameday == -1) {
                    logger.info("GAMEDAY -1");
                    return;
                }
                int gdi = gameday - 1 - 5;
                List<String> users = Arrays.asList(uid1, uid2);
                int i = 0;
                int leagu = league.getLongListList("table1").stream().anyMatch(l -> l.contains(Long.parseLong(uid1))) ? 1 : 2;
                String lea = leagu == 1 ? "Sand" : "Regen";
                List<Long> table = league.getLongListList("table" + leagu).stream().map(l -> l.get(0)).toList();
                RequestBuilder b = new RequestBuilder(sid);
                for (String uid : users) {
                    int index = table.indexOf(Long.parseLong(uid));
                    ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                    List<List<Object>> list = new ArrayList<>();
                    int x = 0;
                    for (String pick : picks) {
                        String kill = getNumber(kills.get(i), pick);
                        String death = getNumber(deaths.get(i), pick);
                        list.add(Arrays.asList(
                                death.equals("") ? "-" : 1,
                                kill.equals("") ? "-" : Integer.parseInt(kill),
                                death.equals("") ? "-" : Integer.parseInt(death)
                        ));
                        x++;
                    }
                    if (game[i].isWinner()) {
                        b.addSingle("Teamseite %s!%s%d".formatted(lea, getAsXCoord(gdi * 3 + 12), index * 15 + 16), "1");
                        if (!league.has("results"))
                            league.put("results", new JSONObject());
                        league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                    }
                    try {
                        b.addAll("Teamseite %s!%s%d".formatted(lea, getAsXCoord(gdi * 3 + 11), index * 15 + 4), list);
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                    i++;
                }
                generateResult(b, game, league, gameday, uid1, "Spielplan", "WoolooCupS4", (String) args[1], leagu);
                b.withRunnable(() -> sortWoolooS4(sid, leagu, league), 3000)
                        .execute();
                saveEmolgaJSON();
            });

            sdAnalyser.put(FPLID, (game, uid1, uid2, kills, deaths, args) -> {
                JSONObject league;
                JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
                int l;
                if (drafts.getJSONObject("FPLS1L1").getLongList("table").contains(Long.parseLong(uid1))) l = 1;
                else l = 2;
                league = drafts.getJSONObject("FPLS1L" + l);
                String sid = league.getString("sid");
                int gameday = getGameDay(league, uid1, uid2);
                if (gameday == -1) {
                    logger.info("GAMEDAY -1");
                    return;
                }
                List<String> users = Arrays.asList(uid1, uid2);
                int i = 0;
                List<Long> table = league.getLongList("table");
                RequestBuilder b = new RequestBuilder(sid);
                String li = "Kader L" + l + "!";
                for (String uid : users) {
                    int index = table.indexOf(Long.parseLong(uid));
                    ArrayList<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                    List<Object> list = new ArrayList<>();
                    int x = 0;
                    for (String pick : picks) {
                        list.add(getNumber(kills.get(i), pick));
                        x++;
                    }
                    int win = 0;
                    int loose = 0;
                    if (game[i].isWinner()) {
                        win++;
                        if (!league.has("results"))
                            league.put("results", new JSONObject());
                        league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                    } else loose++;
                    try {
                        String s = li + getAsXCoord(index / 4 * 22 + 6 + gameday);
                        int yc = index % 4 * 20;
                        b.addColumn(s + (yc + 8), list);
                        b.addSingle(s + (yc + 20), game[i].getTotalDeaths());
                        b.addColumn(s + (yc + 21), Arrays.asList(win, loose));
                    } catch (IllegalArgumentException IllegalArgumentException) {
                        IllegalArgumentException.printStackTrace();
                    }
                    i++;
                }
                //generateResult(b, game, league, gameday, uid1, "Spielplan " + li, "ZBS", (String) args[1]);
                int gdi = gameday - 1;
                List<Object> gpl = new ArrayList<>(Arrays.asList(6 - game[0].getTotalDeaths(),
                        "=HYPERLINK(\"%s\"; \":\")".formatted(args[1]),
                        6 - game[1].getTotalDeaths()));
                String battleorder = league.getJSONObject("battleorder").getString(gameday);
                int ycoord = 0;
                String str = null;
                for (String s : battleorder.split(";")) {
                    if (s.contains(uid1)) {
                        str = s;
                        break;
                    }
                    ycoord++;
                }
                if (str.split(":")[0].equals(uid2)) Collections.reverse(gpl);
                b.addRow("Spielplan (Spoiler) L%d!%s%d".formatted(l, getAsXCoord(gdi / 4 * 6 + 4), gdi % 4 * 6 + 6 + ycoord), gpl);
                b.withRunnable(() -> sortFPL(sid, "Tabelle L" + l, league), 4000).execute();
                saveEmolgaJSON();
            });


            sdAnalyser.put(NDSID, (game, uid1, uid2, kills, deaths, args) -> {
                JSONObject json = getEmolgaJSON();
                JSONObject league = json.getJSONObject("drafts").getJSONObject("NDS");
                String sid = league.getString("sid");
                List<String> users = Arrays.asList(uid1, uid2);
                int gameday = getGameDay(league, uid1, uid2);
                if (gameday == -1) {
                    sendToMe("Gameday -1 " + uid1 + " " + uid2);
                    return;
                }
                ((TextChannel) args[3]).sendMessage("Spieltag " + (gameday + 5) + "\n\n" + args[2]).queue();
                String battle = null;
                int battleindex = -1;
                List<String> battleorder = Arrays.asList(league.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";"));
                for (String s : battleorder) {
                    if (s.contains(uid1)) {
                        battle = s;
                        battleindex = battleorder.indexOf(s);
                        break;
                    }
                }
                ArrayList<String>[] mons = (ArrayList<String>[]) args[0];
                int i = 0;
                RequestBuilder b = new RequestBuilder(sid);
                int gdi = gameday - 1;
                List<String> killlistloc = null;
                try {
                    killlistloc = Files.readAllLines(Paths.get("ndskilllistorder.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (String uid : users) {
                    List<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
                    String str = "'" + league.getJSONObject("teamnames").getString(uid) + "'!";
                    boolean isRight = battle.split(":")[1].equals(uid);
                    List<List<Object>> list = new LinkedList<>();
                    int num = 0;
                    for (String s : mons[i]) {
                        int x = indexPick(picks, s);
                        String kill = getNumber(kills.get(i), s);
                        String death = getNumber(deaths.get(i), s);
                        String killloc = str + getAsXCoord(gameday + 5 + 1) + (x + 200);
                        int killint = kill.equals("") ? 0 : Integer.parseInt(kill);
                        b.addSingle(killloc, killint);
                        b.addSingle("Killliste!%s%d".formatted(getAsXCoord(gameday + 5 + 18), killlistloc.indexOf(s) + 1001), killint);
                        int deathint = death.equals("") ? 0 : Integer.parseInt(death);
                        b.addSingle(str + getAsXCoord(gameday + 5 + 13) + (x + 200), deathint);
                        List<Object> l = new LinkedList<>();
                        l.add(s);
                        l.add(getSerebiiIcon(s));
                        l.add("=" + killloc);
                        if (isRight) Collections.reverse(l);
                        list.add(l);
                        String loc = getAsXCoord(gdi * 9 + (isRight ? 9 : 1)) + (battleindex * 10 + 6 + num);
                        String range = loc + ":" + loc;
                        if (deathint == 1) {
                            b.addFGColorChange(1634614187, range, convertColor(0x000000));
                            b.addStrikethroughChange(1634614187, range, true);
                        } else {
                            b.addFGColorChange(1634614187, range, convertColor(0xefefef));
                            b.addStrikethroughChange(1634614187, range, false);
                        }
                        num++;
                    }
                    b.addAll("Spielplan RR!" + getAsXCoord(gdi * 9 + (isRight ? 7 : 1)) + (battleindex * 10 + 6), list);
                    b.addSingle(str + getAsXCoord(gameday + 3 + 5) + "10", (6 - game[i].getTotalDeaths()) + ":" + (6 - game[1 - i].getTotalDeaths()));
                    //logger.info(uid);
                    //logger.info("slist = " + slist);
                    int win = 0;
                    int loose = 0;
                    if (!league.has("results")) league.put("results", new JSONObject());
                    if (game[i].isWinner()) {
                        win = 1;
                        league.getJSONObject("results").put(uid1 + ":" + uid2, uid);
                        //logger.info("win = " + win);
                    } else {
                        loose = 1;
                        //logger.info("loose = " + loose);
                    }
                    b.addSingle(str + getAsXCoord(gameday + 1 + 5) + "216", win);
                    b.addSingle(str + getAsXCoord(gameday + 13 + 5) + "216", loose);
                    saveEmolgaJSON();
                    i++;
                }
                if (battle.split(":")[0].equals(uid1)) {
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 4) + (battleindex * 10 + 3), 6 - game[0].getTotalDeaths());
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 5) + (battleindex * 10 + 4), "=HYPERLINK(\"" + args[1] + "\"; \"Link\")");
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 6) + (battleindex * 10 + 3), 6 - game[1].getTotalDeaths());
                } else {
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 4) + (battleindex * 10 + 3), 6 - game[1].getTotalDeaths());
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 5) + (battleindex * 10 + 4), "=HYPERLINK(\"" + args[1] + "\"; \"Link\")");
                    b.addSingle("Spielplan RR!" + getAsXCoord(gdi * 9 + 6) + (battleindex * 10 + 3), 6 - game[0].getTotalDeaths());
                }
                b.withRunnable(() -> sortNDS(sid, league), 3000).execute();
            });
        }, "Command Initialization").start();
    }

    public static com.google.api.services.sheets.v4.model.Color convertColor(int hexcode) {
        java.awt.Color c = new java.awt.Color(hexcode);
        return new com.google.api.services.sheets.v4.model.Color()
                .setRed((float) c.getRed() / (float) 255)
                .setGreen((float) c.getGreen() / (float) 255)
                .setBlue((float) c.getBlue() / (float) 255);
    }

    private static List<String> getSortedListOfMons(List<JSONObject> list) {
        Comparator<JSONObject> comp = Comparator.<JSONObject, Integer>comparing(p1 -> Tierlist.getByGuild(Constants.ASLID).order.indexOf(p1.getString("tier"))).thenComparing(p -> p.getString("name"));
        list.sort(comp);
        return list.stream().map(o -> o.getString("name")).collect(Collectors.toList());
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

    public static void generateResult(RequestBuilder b, Player[] game, JSONObject league, int gameday, String uid1, String sheet, String leaguename, String replay, Object... args) {
        int aliveP1 = 0;
        int aliveP2 = 0;
        for (Pokemon p : game[0].getMons()) {
            if (!p.isDead()) aliveP1++;
        }
        for (Pokemon p : game[1].getMons()) {
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
        String coords = switch (leaguename) {
            case "ZBS" -> getZBSGameplanCoords(gameday, index);
            case "Wooloo" -> getWoolooGameplanCoords(gameday, index);
            case "WoolooCupS4" -> getWoolooS4GameplanCoords(gameday, index, (Integer) args[0]);
            case "ASLS10" -> getASLS10GameplanCoords(gameday, index);
            default -> null;
        };
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

    private static String getZBSGameplanCoords(int gameday, int index) {
        if (gameday < 4) return "C" + (gameday * 5 + index - 2);
        if (gameday < 7) return "F" + ((gameday - 3) * 5 + index - 2);
        return "I" + (index + 3);
    }

    private static String getWoolooGameplanCoords(int gameday, int index) {
        logger.info("gameday = " + gameday);
        logger.info("index = " + index);
        if (gameday < 4) return "C" + (gameday * 6 + index - 2);
        if (gameday < 7) return "F" + ((gameday - 3) * 6 + index - 2);
        return "I" + ((gameday - 6) * 6 + index - 2);
    }

    private static String getWoolooS4GameplanCoords(int gameday, int index, int liga) {
        int x = gameday - 1;
        logger.info("gameday = " + gameday);
        logger.info("index = " + index);
        return "%s%d".formatted(getAsXCoord(x > 1 && x < 8 ? gameday % 3 * 4 + 3 : x % 2 * 4 + 5), gameday / 3 * 8 + index + 3);
    }

    private static String getASLS10GameplanCoords(int gameday, int index) {
        int x = gameday - 1;
        logger.info("gameday = " + gameday);
        logger.info("index = " + index);
        return "%s%d".formatted(getAsXCoord(x > 1 && x < 8 ? gameday % 3 * 4 + 3 : x % 2 * 4 + 5), gameday / 3 * 6 + index + 4);
    }

    private static String getASLGameplanCoords(int gameday, int index, int pk) {
        return getAsXCoord(gameday * 5 - 3) + (index * 6 + pk + 3);
    }

    public static void loadJSONFiles() {
        tokens = load("./tokens.json");
        new Thread(() -> {
            emolgajson = load("./emolgadata.json");
            //datajson = loadSD("pokedex.ts", 59);
            //movejson = loadSD("learnsets.ts", 62);
            spritejson = load("./sprites.json");
            shinycountjson = load("./shinycount.json");
            catchrates = load("./catchrates.json");
            TypicalSets.init(load("./typicalsets.json"));
            JSONObject google = tokens.getJSONObject("google");
            Google.setCredentials(google.getString("refreshtoken"), google.getString("clientid"), google.getString("clientsecret"));
            Google.generateAccessToken();
        }, "JSON Fileload").start();
    }

    public static List<Command> getWithCategory(CommandCategory category, Guild g, Member mem) {
        return commands.values().stream().filter(c -> !c.disabled && c.category == category && c.allowsGuild(g) && c.allowsMember(mem))
                .sorted(Comparator.comparing(Command::getName)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void updatePresence() {
        //jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching(lastPresence ? ("auf " + Database.getData("statistics", "count", "name", "analysis") + " Replays") : ("zu @%s".formatted(Constants.MYTAG))));
        //lastPresence = !lastPresence;
        int count = (int) Database.getData("statistics", "count", "name", "analysis");
        replayCount.set(count);
        if (count % 100 == 0) {
            emolgajda.getTextChannelById(904481960527794217L).sendMessage(new SimpleDateFormat("dd.MM.yyyy").format(new Date()) + ": " + count).queue();
        }
        emolgajda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("auf " + count + " analysierte Replays"));
    }

    public static String getHelpDescripion(Guild g, Member mem) {
        StringBuilder s = new StringBuilder();
        for (CommandCategory cat : CommandCategory.getOrder()) {
            if (cat.allowsGuild(g) && cat.allowsMember(mem) && getWithCategory(cat, g, mem).size() > 0)
                s/*.append(cat.isEmote() ? g.getJDA().getEmoteById(cat.emoji).getAsMention() : cat.emoji).append(" ")*/.append(cat.name).append("\n");
        }
        //s.append("\u25c0\ufe0f Zurück zur Übersicht");
        return s.toString();
    }

    public static List<ActionRow> getHelpButtons(Guild g, Member mem) {
        return getActionRows(CommandCategory.getOrder().stream().filter(cat -> cat.allowsGuild(g) && cat.allowsMember(mem)).collect(Collectors.toList()), s -> Button.primary("help;" + s.getName().toLowerCase(), s.getName()).withEmoji(Emoji.fromEmote(g.getJDA().getEmoteById(s.getEmote()))));
    }

    public static void help(TextChannel tco, Member mem) {
        /*if(mem.getIdLong() != Constants.FLOID) {
            tco.sendMessage("Die Help-Funktion wird momentan umprogrammiert und steht deshalb nicht zur Verfügung.").queue();
            return;
        }*/
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Commands").setColor(Color.CYAN);
        Message m = null;
        //builder.setDescription(getHelpDescripion(tco.getGuild(), mem));
        MessageAction ma = tco.sendMessageEmbeds(builder.build());
        Guild g = tco.getGuild();
        ma.setActionRows(getHelpButtons(g, mem)).queue();
    }

    public static void check(MessageReceivedEvent e) {
        Member mem = e.getMember();
        String msg = e.getMessage().getContentDisplay();
        TextChannel tco = e.getTextChannel();
        long gid = e.getGuild().getIdLong();
        Bot bot = Bot.byJDA(e.getJDA());

        JSONObject cc = getEmolgaJSON().getJSONObject("customcommands");
        if (msg.startsWith("!") && cc.has(msg.toLowerCase().substring(1))) {
            JSONObject o = cc.getJSONObject(msg.toLowerCase().substring(1));
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
        if (bot == Bot.FLEGMON || gid == 745934535748747364L) {
            File dir = new File("audio/clips/");
            for (File file : dir.listFiles()) {
                if (msg.equalsIgnoreCase("!" + file.getName().split("\\.")[0])) {
                    GuildVoiceState voiceState = e.getMember().getVoiceState();
                    boolean pepe = mem.getIdLong() == 349978348010733569L;
                    if (voiceState.inAudioChannel() || pepe) {
                        AudioManager am = e.getGuild().getAudioManager();
                        if (!am.isConnected()) {
                            if (voiceState.inAudioChannel()) {
                                am.openAudioConnection(voiceState.getChannel());
                                am.setConnectionListener(new ConnectionListener() {
                                    @Override
                                    public void onPing(long ping) {

                                    }

                                    @Override
                                    public void onStatusChange(@NotNull ConnectionStatus status) {
                                        logger.info("status = " + status);
                                        if (status == ConnectionStatus.CONNECTED) {
                                            playSound(voiceState.getChannel(), "/home/florian/Discord/audio/clips/hi.mp3", tco);
                                            playSound(voiceState.getChannel(), file.getPath(), tco);
                                        }
                                    }

                                    @Override
                                    public void onUserSpeaking(@NotNull User user, boolean speaking) {

                                    }
                                });
                            }
                        } else {
                            playSound(am.getConnectedChannel(), file.getPath(), tco);
                        }
                    }
                }
            }
        }
        Command command = commands.get(msg.split("\\s+")[0].toLowerCase());
        if (command != null) {
            if (command.disabled || command.onlySlash) return;
            if (!command.checkBot(e.getJDA(), gid)) return;
            PermissionCheck check = command.checkPermissions(gid, mem);
            if (check == PermissionCheck.GUILD_NOT_ALLOWED) return;
            if (check == PermissionCheck.PERMISSION_DENIED) {
                tco.sendMessage(NOPERM).queue();
                return;
            }
            if (mem.getIdLong() != FLOID) {
                if (!command.category.disabled.isEmpty()) {
                    tco.sendMessage(command.category.disabled).queue();
                }
                if (command.wip) {
                    tco.sendMessage("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!").queue();
                    return;
                }
            }
            if (!command.everywhere && !command.category.isEverywhere()) {
                if (command.overrideChannel.containsKey(gid)) {
                    List<Long> l = command.overrideChannel.get(gid);
                    if (!l.contains(e.getChannel().getIdLong())) {
                        if (e.getAuthor().getIdLong() == FLOID) {
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
                            if (e.getAuthor().getIdLong() == FLOID) {
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
                int randnum = new Random().nextInt(4096);
                logger.info("randnum = " + randnum);
                if (randnum == 133) {
                    e.getChannel().sendMessage("No, I don't think I will :^)\n||Gib mal den Command nochmal ein, die Wahrscheinlichkeit, dass diese Nachricht auftritt, liegt bei 1:4096 :D||").queue();
                    sendToMe(e.getGuild().getName() + " " + e.getChannel().getAsMention() + " " + e.getAuthor().getId() + " " + e.getAuthor().getAsMention() + " HAT IM LOTTO GEWONNEN!");
                    return;
                }
                if (command.beta)
                    e.getChannel().sendMessage("Dieser Command befindet sich zurzeit in der Beta-Phase! Falls Fehler auftreten, kontaktiert bitte %s durch einen Ping oder eine PN!".formatted(MYTAG)).queue();
                new GuildCommandEvent(command, e);
            } catch (MissingArgumentException ex) {
                ArgumentManagerTemplate.Argument arg = ex.getArgument();
                if (arg.hasCustomErrorMessage()) tco.sendMessage(arg.getCustomErrorMessage()).queue();
                else {
                    tco.sendMessage("Das benötigte Argument `" + arg.getName() + "`, was eigentlich " + buildEnumeration(arg.getType().getName()) + " sein müsste, ist nicht vorhanden!\n" +
                                    "Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help " + command.getName() + "`.").queue();
                }
                if (mem.getIdLong() != FLOID) {
                    sendToMe("MissingArgument " + tco.getAsMention() + " Server: " + tco.getGuild().getName());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (%s).\n".formatted(MYTAG) + command.getHelp(e.getGuild()) + (mem.getIdLong() == FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
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
        //logger.info("getAllForms mon = " + mon.toString(4));
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

    public static String canLearnNDS(String monId, String... moveId) {
        for (String s : moveId) {
            if (canLearn(monId, s)) return "JA";
        }
        return "NEIN";
    }

    public static boolean canLearn(String monId, String moveId) {
        try {
            LinkedList<String> already = new LinkedList<>();
            JSONObject movejson = getLearnsetJSON("default");
            JSONObject data = getDataJSON();
            JSONObject o = data.getJSONObject(monId);
            String str;
            if (o.has("baseSpecies")) str = toSDName(o.getString("baseSpecies"));
            else str = monId;
            while (str != null) {
                JSONObject learnset = movejson.getJSONObject(str).getJSONObject("learnset");
                ResultSet set = getTranslationList(learnset.keySet(), "default");
                while (set.next()) {
                    String moveengl = set.getString("englishid");
                    if (moveengl.equals(moveId)) return true;
                }
                JSONObject mon = data.getJSONObject(str);
                if (mon.has("prevo")) {
                    str = toSDName(mon.getString("prevo"));
                } else str = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
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
                    //logger.info("moveengl = " + moveengl);
                    String moveengl = set.getString("englishid");
                    String move = set.getString("germanname");
                    //logger.info("move = " + move);
                    if ((type.equals("") || atkdata.getJSONObject(moveengl).getString("type").equals(getEnglName(type))) &&
                        (dmgclass.equals("") || atkdata.getJSONObject(moveengl).getString("category").equals(dmgclass)) &&
                        (!msg.toLowerCase().contains("--prio") || atkdata.getJSONObject(moveengl).getInt("priority") > 0) &&
                        containsGen(learnset, moveengl, maxgen) &&
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

        } catch (Exception ex) {
            sendToMe("Schau in die Konsole du kek!");
            ex.printStackTrace();
        }
        return already;
    }

    public static void sendToMe(String msg, Bot... bot) {
        sendToUser(FLOID, msg, bot);
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

    private static String getSerebiiForm(String forme) {
        if (Helpers.isNumeric(forme)) {
            return forme;
        }
        if (forme.equals("Fan")) return "s";
        return String.valueOf(forme.toLowerCase().charAt(0));
    }

    public static String getSerebiiIcon(JSONObject o) {
        int num = o.getInt("num");
        return "=IMAGE(\"https://www.serebii.net/pokedex-swsh/icon/" + getWithZeros(num, 3) + (o.has("forme") ? "-" + getSerebiiForm(o.getString("forme")) : "") + ".png\"; 1)";
    }

    public static String getSerebiiIcon(String str) {
        return getSerebiiIcon(getDataJSON().getJSONObject(getSDName(str)));
    }

    public static String getGen5SpriteWithoutGoogle(JSONObject o, boolean shiny) {
        return "https://play.pokemonshowdown.com/sprites/gen5" + (shiny ? "-shiny" : "") + "/" + toSDName(o.has("baseSpecies") ? o.getString("baseSpecies") : o.getString("name")) + (o.has("forme") ? "-" + toSDName(o.getString("forme")) : "") + ".png";
    }

    public static String getGen5SpriteWithoutGoogle(JSONObject o) {
        return getGen5SpriteWithoutGoogle(o, false);
    }

    public static String getGen5Sprite(JSONObject o) {
        return "=IMAGE(\"https://play.pokemonshowdown.com/sprites/gen5/" + toSDName(o.has("baseSpecies") ? o.getString("baseSpecies") : o.getString("name")) + (o.has("forme") ? "-" + toSDName(o.getString("forme")) : "") + ".png\"; 1)";
    }

    public static String getGen5Sprite(String str) {
        return getGen5Sprite(getDataJSON().getJSONObject(getSDName(str)));
    }

    public static <T> List<ActionRow> getActionRows(Collection<T> c, Function<T, Button> mapper) {
        LinkedList<Button> currRow = new LinkedList<>();
        LinkedList<ActionRow> rows = new LinkedList<>();
        for (T s : c) {
            currRow.add(mapper.apply(s));
            if (currRow.size() == 5) {
                rows.add(ActionRow.of(currRow));
                currRow.clear();
            }
        }
        if (currRow.size() > 0) rows.add(ActionRow.of(currRow));
        return rows;
    }

    public static <T> Collection<T> addAndReturn(Collection<T> c, T toadd) {
        c.add(toadd);
        return c;
    }

    private static List<TextChannel> resultChannelASL(long uid1, long uid2) {
        for (int i = 1; i <= 5; i++) {
            JSONObject league = emolgajson.getJSONObject("drafts").getJSONObject("ASLS10L" + i);
            List<Long> table = league.getLongList("table");
            if (table.containsAll(List.of(uid1, uid2)))
                return Arrays.asList(emolgajda.getTextChannelById(league.getLong("replay")), emolgajda.getTextChannelById(league.getLong("result")));
        }
        return Collections.emptyList();
    }

    public static void analyseReplay(String url, TextChannel customReplayChannell, TextChannel resultchannell, Message m, DeferredSlashResponse e) {
        Player[] game;
        /*if(resultchannel.getGuild().getIdLong() != MYSERVER) {
            (m != null ? m.getChannel() : resultchannel).sendMessage("Ich befinde mich derzeit im Wartungsmodus, versuche es später noch einmal :)").queue();
            return;
        }*/
        logger.info("REPLAY! Channel: {}", m != null ? m.getChannel().getId() : resultchannell.getId());
        try {
            game = new Analysis(url, m).analyse();
            //game = Analysis.analyse(url, m);
        } catch (Exception ex) {
            String msg = "Beim Auswerten des Replays ist (vermutlich wegen eines Zoruas/Zoroarks) ein Fehler aufgetreten! Bitte trage das Ergebnis selbst ein und melde dich gegebenenfalls bei %s!".formatted(MYTAG);
            if (e != null)
                e.reply(msg);
            else {
                resultchannell.sendMessage(msg).queue();
            }
            ex.printStackTrace();
            return;
        }
        Guild g = resultchannell.getGuild();
        long gid;
        String msg = m != null ? m.getContentDisplay() : "";
        if (m != null && m.getAuthor().getIdLong() == FLOID) {
            if (msg.contains("518008523653775366")) gid = 518008523653775366L;
            else if (msg.contains("709877545708945438")) gid = 709877545708945438L;
            else if (msg.contains("747357029714231299")) gid = 747357029714231299L;
            else if (msg.contains("736555250118295622")) gid = 736555250118295622L;
            else if (msg.contains("837425304896536596")) gid = 837425304896536596L;
            else if (msg.contains("860253715624361996")) gid = 860253715624361996L;
            else gid = g.getIdLong();
        } else {
            gid = g.getIdLong();
        }
        String u1 = game[0].getNickname();
        String u2 = game[1].getNickname();
        long uid1 = DBManagers.SD_NAMES.getIDByName(u1);
        long uid2 = DBManagers.SD_NAMES.getIDByName(u2);
        List<TextChannel> aslChannel = resultChannelASL(uid1, uid2);
        if (gid == ASLID && aslChannel.isEmpty()) {
            sendToMe("Invalid ASL Replay");
            return;
        }
        TextChannel customReplayChannel = gid != ASLID ? customReplayChannell : aslChannel.get(0);
        TextChannel resultchannel = g.getIdLong() != ASLID ? resultchannell : aslChannel.get(1);
        logger.info("Analysed!");
        //logger.info(g.getName() + " -> " + (m.isFromType(ChannelType.PRIVATE) ? "PRIVATE " + m.getAuthor().getId() : m.getTextChannel().getAsMention()));
        for (Pokemon p : game[0].getMons()) {
            game[0].addTotalKills(p.getKills());
            game[0].addTotalDeaths(p.isDead() ? 1 : 0);
        }
        for (Pokemon p : game[1].getMons()) {
            game[1].addTotalKills(p.getKills());
            game[1].addTotalDeaths(p.isDead() ? 1 : 0);
        }
        int aliveP1 = game[0].getTeamsize() - game[0].getTotalDeaths();
        int aliveP2 = game[1].getTeamsize() - game[1].getTotalDeaths();
        StringBuilder t1 = new StringBuilder();
        StringBuilder t2 = new StringBuilder();
        String winloose = aliveP1 + ":" + aliveP2;
        boolean p1wins = game[0].isWinner();
        List<Map<String, String>> kills = Arrays.asList(new HashMap<>(), new HashMap<>());
        List<Map<String, String>> deaths = Arrays.asList(new HashMap<>(), new HashMap<>());
        ArrayList<String> p1mons = new ArrayList<>();
        ArrayList<String> p2mons = new ArrayList<>();
        boolean spoiler = spoilerTags.contains(gid);
        TypicalSets typicalSets = TypicalSets.getInstance();
        if (spoiler) t1.append("||");
        for (Pokemon p : game[0].getMons()) {
            logger.info("p.getPokemon() = " + p.getPokemon());
            String monName = getMonName(p.getPokemon(), gid);
            if (monName.trim().endsWith("-")) {
                sendToMe(p.getPokemon() + " SD - at End");
            }
            logger.info("monName = " + monName);
            kills.get(0).put(monName, String.valueOf(p.getKills()));
            deaths.get(0).put(monName, p.isDead() ? "1" : "0");
            p1mons.add(monName);
            if (gid != MYSERVER) {
                DBManagers.FULL_STATS.add(monName, p.getKills(), p.isDead() ? 1 : 0, game[0].isWinner());
                typicalSets.add(monName, p.getMoves(), p.getItem(), p.getAbility());
            }
            t1.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (p1wins || spoiler) ? "X" : "").append("\n");
        }
        for (int i = 0; i < game[0].getTeamsize() - game[0].getMons().size(); i++) {
            t1.append("_unbekannt_").append("\n");
        }
        if (spoiler) t1.append("||");
        if (spoiler) t2.append("||");
        for (Pokemon p : game[1].getMons()) {
            String monName = getMonName(p.getPokemon(), gid);
            if (monName.trim().endsWith("-")) {
                sendToMe(p.getPokemon() + " SD - at End");
            }
            kills.get(1).put(monName, String.valueOf(p.getKills()));
            deaths.get(1).put(monName, p.isDead() ? "1" : "0");
            p2mons.add(monName);
            if (gid != MYSERVER) {
                DBManagers.FULL_STATS.add(monName, p.getKills(), p.isDead() ? 1 : 0, game[1].isWinner());
                typicalSets.add(monName, p.getMoves(), p.getItem(), p.getAbility());
            }
            t2.append(monName).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && (!p1wins || spoiler) ? "X" : "").append("\n");
        }
        for (int i = 0; i < game[1].getTeamsize() - game[1].getMons().size(); i++) {
            t2.append("_unbekannt_").append("\n");
        }
        if (spoiler) t2.append("||");
        logger.info("Kills");
        logger.info(String.valueOf(kills));
        logger.info("Deaths");
        logger.info(String.valueOf(deaths));
        String name1;
        String name2;
        JSONObject json = getEmolgaJSON();
        //JSONObject showdown = json.getJSONObject("showdown").getJSONObject(String.valueOf(gid));
        logger.info("u1 = " + u1);
        logger.info("u2 = " + u2);
            /*for (String s : showdown.keySet()) {
                if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
            }*/

        //JSONObject teamnames = json.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("teamnames");
        name1 = /*uid1 != -1 && gid == NDSID ? teamnames.getString(String.valueOf(uid1)) : */game[0].getNickname();
        name2 = /*uid2 != -1 && gid == NDSID ? teamnames.getString(String.valueOf(uid2)) : */game[1].getNickname();
        logger.info("uid1 = " + uid1);
        logger.info("uid2 = " + uid2);
        String str;
        if (spoiler) {
            str = name1 + " ||" + winloose + "|| " + name2 + "\n\n" + name1 + ":\n" + t1
                  + "\n" + name2 + ": " + "\n" + t2;
        } else {
            str = name1 + " " + winloose + " " + name2 + "\n\n" + name1 + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1
                  + "\n" + name2 + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2;
        }
        if (customReplayChannel != null) customReplayChannel.sendMessage(url).queue();
        if (e != null) {
            e.reply(str);
        } else if (!customResult.contains(gid))
            resultchannel.sendMessage(str).queue();
        Database.incrementStatistic("analysis");
        for (int i = 0; i < 2; i++) {
            if (game[i].getMons().stream().anyMatch(mon -> mon.getPokemon().equals("Zoroark") || mon.getPokemon().equals("Zorua")))
                resultchannel.sendMessage("Im Team von " + game[i].getNickname() + " befindet sich ein Zorua/Zoroark! Bitte noch einmal die Kills überprüfen!").queue();
        }
        logger.info("In Emolga Listener!");
        //if (gid != 518008523653775366L && gid != 447357526997073930L && gid != 709877545708945438L && gid != 736555250118295622L && )
        //  return;
        typicalSets.save();
        if (uid1 == -1 || uid2 == -1) return;
        if (sdAnalyser.containsKey(gid)) {
            sdAnalyser.get(gid).analyse(game, String.valueOf(uid1), String.valueOf(uid2), kills, deaths, new ArrayList[]{p1mons, p2mons}, url, str, resultchannel, t1, t2, customReplayChannel, m);
        }
    }

    public static String getIconSprite(String str) {
        logger.info("s = " + str);
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
        logger.info("url = " + url);
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

    public static long calculateDraftTimer() {
        //long delay = calculateTimer(10, 22, 120);
        long delay = calculateTimer(DraftTimer.NDS);
        logger.info(MarkerFactory.getMarker("important"), "delay = {}", delay);
        logger.info(MarkerFactory.getMarker("important"), "expires = {}", delay + System.currentTimeMillis());
        return delay;
    }

    public static long calculateTimer(DraftTimer draftTimer) {
        TimerInfo data = draftTimer.getTimerInfo();
        int delayinmins = draftTimer.getDelayInMins();
        Calendar cal = Calendar.getInstance();
        long currentTimeMillis = cal.getTimeInMillis();
        int elapsedMinutes = delayinmins;
        while (elapsedMinutes > 0) {
            TimerData p = data.get(cal.get(DAY_OF_WEEK));
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour >= p.from() && hour < p.to()) elapsedMinutes--;
            else if (elapsedMinutes == delayinmins) cal.set(SECOND, 0);
            cal.add(Calendar.MINUTE, 1);
        }
        return cal.getTimeInMillis() - currentTimeMillis;
    }

    public static long getXPNeeded(int level) {
        return (long) (5 * Math.pow(level, 2) + 50L * level + 100);
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

    public static Translation getDraftGerName(String s) {
        logger.info("getDraftGerName s = " + s);
        Translation gerName = getGerName(s);
        if (gerName.isSuccess()) return gerName;
        String[] split = s.split("-");
        logger.info("getDraftGerName Arr = " + Arrays.toString(split));
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
        logger.info("split[0] = " + split[0]);
        Translation t = getGerName(split[0]);
        System.out.print("DraftGer Trans ");
        t.print();
        if (t.isSuccess()) {
            Translation tr = t.append("-" + split[1]);
            logger.info("getDraftGerName ret = " + tr);
            return tr;
        }
        return Translation.empty();
    }

    public static String getGerNameWithForm(String name) {
        StringBuilder toadd = new StringBuilder(name);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(toadd.toString().split("-")));
        if (toadd.toString().contains("-Alola")) {
            toadd = new StringBuilder("Alola-" + getGerNameNoCheck(split.get(0)));
            for (int i = 2; i < split.size(); i++) {
                toadd.append("-").append(split.get(i));
            }
        } else if (toadd.toString().contains("-Galar")) {
            toadd = new StringBuilder("Galar-" + getGerNameNoCheck(split.get(0)));
            for (int i = 2; i < split.size(); i++) {
                toadd.append("-").append(split.get(i));
            }
        } else if (toadd.toString().contains("-Mega")) {
            toadd = new StringBuilder("Mega-" + getGerNameNoCheck(split.get(0)));
            for (int i = 2; i < split.size(); i++) {
                toadd.append("-").append(split.get(i));
            }
        } else if (split.size() > 1) {
            toadd = new StringBuilder(getGerNameNoCheck(split.remove(0)) + "-" + String.join("-", split));
        } else toadd = new StringBuilder(getGerNameNoCheck(toadd.toString()));
        return toadd.toString();
    }

    public static Translation getGerName(String s) {
        return getGerName(s, "default", false);
    }

    public static Translation getGerName(String s, String mod, boolean checkOnlyEnglish) {
        logger.info("getGerName s = " + s);
        String id = toSDName(s);
        if (translationsCacheGerman.containsKey(id)) return translationsCacheGerman.get(id);
        ResultSet set = getTranslation(id, mod, checkOnlyEnglish);
        try {
            if (set.next()) {
                Translation t = new Translation(set.getString("germanname"), Translation.Type.fromId(set.getString("type")), Translation.Language.GERMAN, set.getString("englishname"), set.getString("forme"));
                addToCache(true, id, t);
                return t;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Translation.empty();
    }

    public static String getGerNameNoCheck(String s) {
        return getGerName(s).getTranslation();
    }

    public static void addToCache(boolean german, String sd, Translation t) {
        if (german) {
            translationsCacheGerman.put(sd, t);
            translationsCacheOrderGerman.add(sd);
            if (translationsCacheOrderGerman.size() > 1000) {
                translationsCacheGerman.remove(translationsCacheOrderGerman.removeFirst());
            }
        } else {
            translationsCacheEnglish.put(sd, t);
            translationsCacheOrderEnglish.add(sd);
            if (translationsCacheOrderEnglish.size() > 1000) {
                translationsCacheEnglish.remove(translationsCacheOrderEnglish.removeFirst());
            }
        }
    }

    public static void removeNickFromCache(String sd) {
        translationsCacheGerman.remove(sd);
        translationsCacheEnglish.remove(sd);
        translationsCacheOrderGerman.remove(sd);
        translationsCacheOrderEnglish.remove(sd);
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
        String id = toSDName(s);
        if (translationsCacheEnglish.containsKey(id)) return translationsCacheEnglish.get(id);
        ResultSet set = getTranslation(id, mod);
        try {
            if (set.next()) {
                Translation t = new Translation(set.getString("englishname"), Translation.Type.fromId(set.getString("type")), Translation.Language.ENGLISH, set.getString("germanname"), set.getString("forme"));
                addToCache(false, id, t);
                return t;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Translation.empty();
    }

    public static ResultSet getTranslation(String s, String mod) {
        return getTranslation(s, mod, false);
    }

    public static ResultSet getTranslation(String s, String mod, boolean checkOnlyEnglish) {
        return DBManagers.TRANSLATIONS.getTranslation(s, mod, checkOnlyEnglish);
        /*String id = toSDName(s);

        String query = "select * from translations where (englishid=\"" + id + "\" or germanid=\"" + id + "\")" + (mod != null ? " and (modification=\"" + mod + "\"" + (!mod.equals("default") ? " or modification=\"default\"" : "") + ")" : "");
        //logger.info(query);
        return Database.select(query);*/
    }

    public static ResultSet getTranslationList(Collection<String> l, String mod) {
        String query = "select * from translations where (" + l.stream().map(str -> "englishid=\"" + toSDName(str) + "\"").collect(Collectors.joining(" or ")) + ")" + (mod != null ? " and (modification=\"" + mod + "\"" + (!mod.equals("default") ? " or modification=\"default\"" : "") + ")" : "");
        return Database.select(query, true);
    }

    public static String getSDName(String s) {
        return getSDName(s, "default");
    }

    public static String getSDName(String str, String mod) {
        logger.info("getSDName s = " + str);
        Optional<String> op = sdex.keySet().stream().filter(str::equalsIgnoreCase).findFirst();
        String gitname;
        if (op.isPresent()) {
            String ex = op.get();
            String englname = getEnglName(ex.split("-")[0]);
            return toSDName(englname + sdex.get(str));
        } else {
            if (str.startsWith("M-")) {
                String sub = str.substring(2);
                if (str.endsWith("-X")) gitname = getEnglName(sub.substring(0, sub.length() - 2)) + "megax";
                else if (str.endsWith("-Y")) gitname = getEnglName(sub.substring(0, sub.length() - 2)) + "megay";
                else gitname = getEnglName(sub) + "mega";
            } else if (str.startsWith("A-")) {
                gitname = getEnglName(str.substring(2)) + "alola";
            } else if (str.startsWith("G-")) {
                gitname = getEnglName(str.substring(2)) + "galar";
            } else if (str.startsWith("Amigento-")) {
                gitname = "silvally" + getEnglName(str.split("-")[1]);
            } else {
                gitname = getEnglName(str);
            }
        }
        return toSDName(gitname);
    }

    public static String toSDName(String s) {
        return s.toLowerCase().replaceAll("[^a-zA-Z0-9äöüÄÖÜß♂♀é+]+", "");
    }

    public static String toUsername(String s) {
        return s.toLowerCase().trim().replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss").replaceAll("[^a-zA-Z0-9]+", "");
    }

    public static String getDataName(String s) {
        logger.info("s = " + s);
        if (s.equals("Wie-Shu")) return "mienshao";
        if (s.equals("Lin-Fu")) return "mienfoo";
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

    public static String getMonName(String s, long gid) {
        return getMonName(s, gid, true);
    }

    public static String getMonName(String s, long gid, boolean withDebug) {
        if (withDebug)
            logger.info("s = " + s);
        if (gid == 709877545708945438L) {
            if (s.endsWith("-Alola")) {
                return "Alola-" + getGerName(s.substring(0, s.length() - 6), "default", true).getTranslation();
            } else if (s.endsWith("-Galar")) {
                return "Galar-" + getGerName(s.substring(0, s.length() - 6), "default", true).getTranslation();
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
        if (s.equals("Greninja-Ash")) return "Quajutsu-Ash";
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
        if ((gid == ASLID || gid == NDSID || s.equals("Urshifu-Rapid-Strike")) && s.contains("Urshifu"))
            return "Wulaosu-Wasser";
        if (s.equals("Urshifu")) return "Wulaosu-Unlicht";
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
        if (s.equals("Eevee-Starter")) return "Evoli-Starter";
        if (s.equals("Pikachu-Starter")) return "Pikachu-Starter";
        if (s.contains("Rotom")) return s;
        if (s.contains("Florges")) return "Florges";
        if (s.contains("Floette")) return "Floette";
        if (s.contains("Flabébé")) return "Flabébé";
        if (s.contains("Silvally")) {
            String[] split = s.split("-");
            if (split.length == 1 || s.equals("Silvally-*")) return "Amigento";
            else if (split[1].equals("Psychic")) return "Amigento-Psycho";
            else return "Amigento-" + getGerName(split[1], "default", true).getTranslation();
        }
        if (s.contains("Arceus")) {
            String[] split = s.split("-");
            if (split.length == 1 || s.equals("Arceus-*")) return "Arceus";
            else if (split[1].equals("Psychic")) return "Arceus-Psycho";
            else return "Arceus-" + getGerName(split[1], "default", true).getTranslation();
        }
        if (s.contains("Basculin")) return "Barschuft";
        if (s.contains("Sawsbuck")) return "Kronjuwild";
        if (s.contains("Deerling")) return "Sesokitz";
        if (s.equals("Kyurem-Black")) return "Kyurem-Black";
        if (s.equals("Kyurem-White")) return "Kyurem-White";
        if (s.equals("Meowstic")) return "Psiaugon-M";
        if (s.equals("Meowstic-F")) return "Psiaugon-W";
        if (s.equalsIgnoreCase("Hoopa-Unbound")) return "Hoopa-U";
        if (s.equals("Zygarde")) return "Zygarde-50%";
        if (s.equals("Zygarde-10%")) return "Zygarde-10%";
        if (s.endsWith("-Mega")) {
            return "M-" + getGerName(s.substring(0, s.length() - 5), "default", true).getTranslation();
        } else if (s.endsWith("-Alola")) {
            return "A-" + getGerName(s.substring(0, s.length() - 6), "default", true).getTranslation();
        } else if (s.endsWith("-Galar")) {
            return "G-" + getGerName(s.substring(0, s.length() - 6), "default", true).getTranslation();
        } else if (s.endsWith("-NML")) {
            return "NML-" + getGerName(s.substring(0, s.length() - 6), "default", true).getTranslation();
        } else if (s.endsWith("-Therian")) {
            return getGerName(s.substring(0, s.length() - 8), "default", true).getTranslation() + "-T";
        } else if (s.endsWith("-X")) {
            return "M-" + getGerName(s.split("-")[0], "default", true).getTranslation() + "-X";
        } else if (s.endsWith("-Y")) {
            return "M-" + getGerName(s.split("-")[0], "default", true).getTranslation() + "-Y";
        }
        if (s.equals("Tornadus")) return "Boreos-I";
        if (s.equals("Thundurus")) return "Voltolos-I";
        if (s.equals("Landorus")) return "Demeteros-I";
        Translation gername = getGerName(s, "default", true);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(s.split("-")));
        if (gername.isFromType(Translation.Type.POKEMON)) {
            return gername.getTranslation();
        }
        String first = split.remove(0);
        return getGerName(first, "default", true).getTranslation() + "-" + String.join("-" + split);
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
        return !emolgaChannel.containsKey(gid) || emolgaChannel.get(gid).contains(tc.getIdLong()) || emolgaChannel.get(gid).isEmpty();
    }

    private void addToMap() {
        String prefix = otherPrefix ? "e!" : "!";
        commands.put(prefix + name, this);
        aliases.forEach(str -> commands.put(prefix + str, this));
    }

    public boolean isSlash() {
        return slash;
    }

    protected void setCustomPermissions(Predicate<Member> predicate) {
        this.allowsMember = predicate.or(member -> member.getIdLong() == FLOID);
        this.customPermissions = true;
    }

    protected void addCustomChannel(long guildId, long... channelIds) {
        List<Long> l = overrideChannel.computeIfAbsent(guildId, k -> new LinkedList<>());
        for (long channelId : channelIds) {
            l.add(channelId);
        }
    }

    protected void disable() {
        this.disabled = true;
    }

    protected void slash(boolean onlySlash) {
        this.onlySlash = onlySlash;
        this.slash = true;
    }

    protected void slash() {
        this.slash(false);
    }

    protected void wip() {
        this.wip = true;
    }

    protected void beta() {
        this.beta = true;
    }

    protected void setAdminOnly() {
        setCustomPermissions(PermissionPreset.ADMIN);
    }

    public boolean allowsMember(Member mem) {
        return category.allowsMember(mem) && !customPermissions || allowsMember.test(mem);
    }

    public boolean allowsGuild(Guild g) {
        return allowsGuild(g.getIdLong());
    }

    public boolean allowsGuild(long gid) {
        if (gid == MYSERVER) return true;
        if (allowedGuilds.isEmpty() && category.allowsGuild(gid)) return true;
        return !allowedGuilds.isEmpty() && allowedGuilds.contains(gid);
    }

    public PermissionCheck checkPermissions(long gid, Member mem) {
        if (!allowsGuild(gid)) return PermissionCheck.GUILD_NOT_ALLOWED;
        if (!allowsMember(mem)) return PermissionCheck.PERMISSION_DENIED;
        return PermissionCheck.GRANTED;
    }

    public boolean checkBot(JDA jda, long guildid) {
        return allowedBotId == -1 || allowedBotId == jda.getSelfUser().getIdLong() || guildid == CULTID;
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

    public Set<String> getAliases() {
        return aliases;
    }

    public ArgumentManagerTemplate getArgumentTemplate() {
        return argumentTemplate;
    }

    public void setArgumentTemplate(ArgumentManagerTemplate template) {
        this.argumentTemplate = template;
    }

    public CommandCategory getCategory() {
        return category;
    }

    public String getHelp(Guild g) {
        ArgumentManagerTemplate args = getArgumentTemplate();
        if (args == null) {
            return "`" + getPrefix() + getName() + "` " + overrideHelp.getOrDefault(g.getIdLong(), help);
        }
        return "`" + (args.hasSyntax() ? args.getSyntax() : getPrefix() + getName() + (args.arguments.size() > 0 ? " " : "")
                                                            + args.arguments.stream().map(a -> (a.isOptional() ? "[" : "<") + a.getName() + (a.isOptional() ? "]" : ">")).collect(Collectors.joining(" "))) + "` " + overrideHelp.getOrDefault(g.getIdLong(), help) + (wip ? " (**W.I.P.**)" : "");
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

        OptionType asOptionType();

        boolean needsValidate();
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

    public record ArgumentManager(HashMap<String, Object> map) {

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

        public boolean isTextIgnoreCase(String key, String text) {
            return ((String) map.getOrDefault(key, "")).equalsIgnoreCase(text);
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

        public Message.Attachment getAttachment(String key) {
            return (Message.Attachment) map.get(key);
        }
    }

    public record SubCommand(String name, String help) {

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

        public static ArgumentManagerTemplate noArgs() {
            return noCheckTemplate;
        }

        public static ArgumentManagerTemplate noSpecifiedArgs(String syntax, String example) {
            return new ArgumentManagerTemplate(new LinkedList<>(), true, example, syntax);
        }

        public static ArgumentType draft() {
            return withPredicate("Draftname", s -> getEmolgaJSON().getJSONObject("drafts").has(s) || getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9").has(s), false);
        }

        public static ArgumentType draftPokemon() {
            return withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper);
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

                @Override
                public OptionType asOptionType() {
                    return OptionType.STRING;
                }

                @Override
                public boolean needsValidate() {
                    return true;
                }
            };
        }

        public static ArgumentType withPredicate(String name, Predicate<String> check, boolean female, Function<String, String> mapper) {
            return new ArgumentType() {
                @Override
                public Object validate(String str, Object... params) {
                    if (check.test(str)) {
                        return mapper.apply(str);
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

                @Override
                public OptionType asOptionType() {
                    return OptionType.STRING;
                }

                @Override
                public boolean needsValidate() {
                    return true;
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

        public ArgumentManager construct(SlashCommandInteractionEvent e) throws ArgumentException {
            HashMap<String, Object> map = new HashMap<>();
            for (Argument arg : arguments) {
                if (e.getOption(arg.getName().toLowerCase()) == null && !arg.isOptional()) {
                    throw new MissingArgumentException(arg);
                }
                ArgumentType type = arg.getType();
                OptionMapping o = e.getOption(arg.getName().toLowerCase());
                Object obj;
                if (type.needsValidate()) {
                    obj = type.validate(o.getAsString(), arg.getLanguage(), getModByGuild(e.getGuild()));
                    if (obj == null) throw new MissingArgumentException(arg);
                } else {
                    switch (o.getType()) {
                        case ROLE -> obj = o.getAsRole();
                        case CHANNEL -> obj = o.getAsGuildChannel();
                        case USER -> obj = o.getAsUser();
                        case INTEGER -> obj = o.getAsLong();
                        case BOOLEAN -> obj = o.getAsBoolean();
                        default -> obj = o.getAsString();
                    }
                }
                map.put(arg.getId(), obj);
            }
            return new ArgumentManager(map);
        }

        public ArgumentManager construct(MessageReceivedEvent e) throws ArgumentException {
            if (noCheck) return null;
            Message m = e.getMessage();
            long mid = m.getIdLong();
            String raw = m.getContentRaw();
            ArrayList<String> split = new ArrayList<>(Arrays.asList(raw.split("\\s+")));
            split.remove(0);
            HashMap<ArgumentType, Integer> asFar = new HashMap<>();
            HashMap<String, Object> map = new HashMap<>();
            int argumentI = 0;
            for (int i = 0; i < split.size(); ) {
                if (i >= arguments.size()) {
                    break;
                }
                if (argumentI >= arguments.size()) break;
                Argument a = arguments.get(argumentI);
                if (a.disabled.contains(mid)) break;
                String str = argumentI + 1 == arguments.size() ? String.join(" ", split.subList(i, split.size())) : split.get(i);
                ArgumentType type = a.getType();
                Object o;
                int count = 1;
                if (type instanceof DiscordType || type instanceof DiscordFile) {
                    o = type.validate(str, m, asFar.getOrDefault(type, 0));
                    if (o != null) asFar.put(type, asFar.getOrDefault(type, 0) + 1);
                } else {
                    o = type.validate(str, a.getLanguage(), getModByGuild(e.getGuild()));
                    if (o == null) {
                        boolean b = true;
                        for (int j = argumentI + 1; j < arguments.size(); j++) {
                            if (!arguments.get(j).isOptional()) {
                                b = false;
                                break;
                            }
                        }
                        if (b && arguments.size() > argumentI + 1) {
                            arguments.get(argumentI + 1).disabled.add(mid);
                        }
                        if (b) {
                            o = type.validate(String.join(" ", split.subList(i, split.size())), a.getLanguage(), getModByGuild(e.getGuild()));
                        }
                    }
                }
                if (o == null) {
                    if (!a.isOptional()) {
                        clearDisable(mid);
                        throw new MissingArgumentException(a);
                    }
                } else {
                    map.put(a.getId(), o);
                    i++;
                }
                argumentI++;
            }
            clearDisable(mid);
            if (arguments.stream().anyMatch(argument -> !argument.optional) && map.size() == 0) {
                throw new MissingArgumentException(arguments.stream().filter(argument -> !argument.optional).findFirst().orElse(null));
            }
            return new ArgumentManager(map);
        }

        private void clearDisable(long l) {
            arguments.forEach(a -> a.disabled.remove(l));
        }


        public enum DiscordType implements ArgumentType {
            USER(Pattern.compile("<@!*\\d{18,22}>"), "User", false, OptionType.USER),
            CHANNEL(Pattern.compile("<#*\\d{18,22}>"), "Channel", false, OptionType.CHANNEL),
            ROLE(Pattern.compile("<@&*\\d{18,22}>"), "Rolle", true, OptionType.ROLE),
            ID(Pattern.compile("\\d{18,22}"), "ID", true, OptionType.STRING),
            INTEGER(Pattern.compile("\\d{1,9}"), "Zahl", true, OptionType.INTEGER);

            final Pattern pattern;
            final String name;
            final boolean female;
            final OptionType optionType;


            DiscordType(Pattern pattern, String name, boolean female, OptionType optionType) {
                this.pattern = pattern;
                this.name = name;
                this.female = female;
                this.optionType = optionType;
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

            @Override
            public OptionType asOptionType() {
                return optionType;
            }

            @Override
            public boolean needsValidate() {
                return false;
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
                if (any) return null;
                return texts.stream().map(sc -> "`" + sc.getName() + "`" + sc.getHelp()).collect(Collectors.joining("\n"));
            }

            @Override
            public OptionType asOptionType() {
                return OptionType.STRING;
            }

            @Override
            public boolean needsValidate() {
                return true;
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

            @Override
            public OptionType asOptionType() {
                return OptionType.INTEGER;
            }

            @Override
            public boolean needsValidate() {
                return true;
            }
        }

        public record DiscordFile(String fileType) implements ArgumentType {

            public static DiscordFile of(String fileType) {
                return new DiscordFile(fileType);
            }

            @Override
            public Object validate(String str, Object... params) {
                List<Message.Attachment> att = ((Message) params[0]).getAttachments();
                if (att.size() == 0) return null;
                Message.Attachment a = att.get((Integer) params[1]);
                if (a.getFileName().endsWith("." + fileType) || fileType.equals("*")) {
                    return a;
                }
                return null;
            }

            @Override
            public String getName() {
                return "eine " + (fileType.equals("*") ? "" : fileType + "-") + "Datei";
            }

            @Override
            public String getCustomHelp() {
                return null;
            }

            @Override
            public OptionType asOptionType() {
                return null;
            }

            @Override
            public boolean needsValidate() {
                return false;
            }
        }

        public static final class Argument {
            public final LinkedList<Long> disabled = new LinkedList<>();
            private final String id;
            private final String name;
            private final String help;
            private final ArgumentType type;
            private final boolean optional;
            private final Translation.Language language;
            private final String customErrorMessage;

            public Argument(String id, String name, String help, ArgumentType type, boolean optional, Translation.Language language, String customErrorMessage) {
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
                if (type.getCustomHelp() == null) return b.toString();
                if (!help.equals("")) {
                    b.append("\n");
                }
                return b.append("Möglichkeiten:\n").append(type.getCustomHelp()).toString();
            }

            public String getId() {
                return id;
            }

            public ArgumentType getType() {
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
        }

        private final Type type;
        private final Language language;
        private final String translation;
        private final boolean empty;
        private final String otherLang;
        private final String forme;

        public Translation(String translation, Type type, Language language) {
            this.translation = translation;
            this.type = type;
            this.language = language;
            this.empty = type == Type.UNKNOWN;
            this.otherLang = "";
            this.forme = null;
        }

        public Translation(String translation, Type type, Language language, String otherLang) {
            this.translation = translation;
            this.type = type;
            this.language = language;
            this.otherLang = otherLang;
            this.empty = false;
            this.forme = null;
        }

        public Translation(String translation, Type type, Language language, String otherLang, String forme) {
            this.translation = translation;
            this.type = type;
            this.language = language;
            this.otherLang = otherLang;
            this.forme = forme;
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

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Translation{");
            sb.append("type=").append(type);
            sb.append(", language=").append(language);
            sb.append(", translation='").append(translation).append('\'');
            sb.append(", empty=").append(empty);
            sb.append(", otherLang='").append(otherLang).append('\'');
            sb.append('}');
            logger.info("ToStringCheck {}", sb);
            return sb.toString();
        }

        public void print() {
            logger.info("Translation{" + "type=" + type +
                        ", language=" + language +
                        ", translation='" + translation + '\'' +
                        ", empty=" + empty +
                        ", otherLang='" + otherLang + '\'' +
                        '}');
        }

        public String getOtherLang() {
            return otherLang;
        }

        public String getForme() {
            return forme != null ? forme : "";
        }

        public Translation append(String str) {
            return new Translation(this.translation + str, this.type, this.language, this.otherLang, this.forme);
        }

        public Translation before(String str) {
            return new Translation(str + this.translation, this.type, this.language, this.otherLang, this.forme);
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
            TRAINER("trainer", "Trainer", false),
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

                    @Override
                    public OptionType asOptionType() {
                        return OptionType.STRING;
                    }

                    @Override
                    public boolean needsValidate() {
                        return true;
                    }
                };
            }

            public static ArgumentType all() {
                return of(Arrays.stream(values()).filter(t -> t != UNKNOWN).toArray(Type[]::new));
            }

            public ArgumentType or(String name) {
                return new ArgumentType() {
                    @Override
                    public Object validate(String str, Object... params) {
                        if (str.equals(name)) return new Translation("Tom", TRAINER, Language.GERMAN, "Tom");
                        return Type.this.validate(str, params);
                    }

                    @Override
                    public String getName() {
                        return Type.this.getName();
                    }

                    @Override
                    public String getCustomHelp() {
                        return Type.this.getCustomHelp();
                    }

                    @Override
                    public OptionType asOptionType() {
                        return Type.this.asOptionType();
                    }

                    @Override
                    public boolean needsValidate() {
                        return Type.this.needsValidate();
                    }
                };
            }

            @Override
            public boolean needsValidate() {
                return true;
            }

            public String getId() {
                return id;
            }

            @Override
            public Object validate(String str, Object... params) {
                String mod = (String) params[1];
                Translation t = params[0] == Language.GERMAN ? getGerName(str, mod, false) : getEnglNameWithType(str, mod);
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

            @Override
            public OptionType asOptionType() {
                return OptionType.STRING;
            }
        }
    }
}
