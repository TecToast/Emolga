package de.tectoast.emolga.bot;

import de.tectoast.emolga.buttons.ButtonListener;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.PrivateCommands;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.showdown.Analysis;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.SDPokemon;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class EmolgaListener extends ListenerAdapter {
    public static final File emolgadata = new File("./emolgadata.json");
    public static final File wikidata = new File("./wikidata.json");
    public static final List<String> allowsCaps = Arrays.asList("712612442622001162", "752230819644440708", "732545253344804914");
    public static final String WELCOMEMESSAGE = "Hallo **{USERNAME}** und vielen Dank, dass du mich auf deinen Server **{SERVERNAME}** geholt hast! " +
            "Vermutlich möchtest du für deinen Server hauptsächlich, dass die Ergebnisse von Showdown Replays in einen Channel geschickt werden. " +
            "**Zunächst pingst du mich auf deinem Server und reagierst mit \uD83C\uDDF8, um die Showdown-Hilfe aufzurufen. " +
            "Dort siehst du, wie man den !replay Command verwendet, um genau das einzustellen.** Falls irgendwelche Probleme oder Fragen auftreten sollten, schreib meinem Programmierer TecToast/Flo eine PN oder nutz den `!flohelp <Nachricht>` Command, mit dem Flo ebenfalls benachrichtigt wird.";
    public static boolean disablesort = false;
    public static File file = new File("./debug.txt");

    static BufferedImage resizeImage(File img) {
        BufferedImage originalImage = null;
        try {
            originalImage = ImageIO.read(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedImage resizedImage = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, 96, 96, null);
        graphics2D.dispose();
        //System.out.println(resizedImage);
        return resizedImage;
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent e) {
        ButtonListener.check(e);
    }


    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent e) {
        Guild g = e.getGuild();
        Member mem = e.getMember();
        long gid = g.getIdLong();
        if (gid == Constants.BSID) {
            g.addRoleToMember(mem, g.getRoleById(715242519528603708L)).queue();
        }
        if (gid == Constants.ASLID) {
            g.getTextChannelById(615605820381593610L).sendMessage(
                    "Willkommen auf der ASL, " + mem.getAsMention() + ". <:hi:540969951608045578>\n" +
                            "Dies ist ein Pokémon Server mit dem Fokus auf einem kompetetiven Draftligasystem. " +
                            "Mach dich mit dem <#635765395038666762> vertraut und beachte die vorgegebenen Themen der Kanäle. Viel Spaß! <:yay:540970044297838597>"
            ).queue();
        }
    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent e) {
        if (e.getMember().getId().equals("634093507388243978")) e.getGuild().kickVoiceMember(e.getMember()).queue();
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent e) {
        e.getGuild().retrieveOwner().flatMap(m -> m.getUser().openPrivateChannel()).queue(pc -> pc.sendMessage(WELCOMEMESSAGE.replace("{USERNAME}", e.getGuild().getOwner().getUser().getName()).replace("{SERVERNAME}", e.getGuild().getName())).queue());
        sendToMe(e.getGuild().getTextChannels().get(0).createInvite().complete().getUrl());
    }

    @Override
    public void onRoleCreate(@Nonnull RoleCreateEvent e) {
        if (!e.getGuild().getId().equals("736555250118295622") && !e.getGuild().getId().equals("447357526997073930") && !e.getGuild().getId().equals("518008523653775366"))
            return;
        e.getRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE).queue();
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent e) {
        if (!e.getAuthor().isBot())
            e.getJDA().getTextChannelById(828044461379682314L).sendMessage(e.getAuthor().getAsMention() + ": " + e.getMessage().getContentDisplay()).queue();
        if (e.getAuthor().getIdLong() == Constants.FLOID) {
            PrivateCommands.execute(e.getMessage());
        }
        String msg = e.getMessage().getContentDisplay();
        long gid = Constants.ASLID;
        Guild g = e.getJDA().getGuildById(gid);
        if (msg.contains("https://") || msg.contains("http://")) {
            Optional<String> urlop = Arrays.stream(msg.split("\n")).filter(s -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/")).map(s -> s.substring(s.indexOf("http"), s.indexOf(" ", s.indexOf("http") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("http") + 1))).findFirst();
            if (urlop.isPresent()) {
                String url = urlop.get();
                analyseASLReplay(e.getAuthor().getIdLong(), url, e.getMessage());
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        if (e.getMember().getId().equals("723829878755164202")) {
            GuildMusicManager manager = getGuildAudioPlayer(e.getGuild());
            manager.scheduler.queue.clear();
            manager.scheduler.nextTrack();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        try {
            Draft.init(e.getJDA());
            if (emolgajson.has("giveaways")) {
                JSONArray arr = emolgajson.getJSONArray("giveaways");
                for (Object o : arr) {
                    JSONObject obj = (JSONObject) o;
                    new Giveaway(obj.getString("tcid"), obj.getString("author"), new Date(obj.getLong("end")).toInstant(), obj.getInt("winners"), obj.getString("prize"), obj.getString("mid"), obj.has("role"));
                }
            }
            /*ResultSet mutes = Database.select("select * from mutes");
            while (mutes.next()) {
                Timestamp ts = mutes.getTimestamp("expires");
                long expires;
                if (ts != null) {
                    expires = ts.getTime();
                } else {
                    expires = -1;
                }
                muteTimer(e.getJDA().getGuildById(mutes.getLong("guildid")), expires, String.valueOf(mutes.getLong("userid")));
            }
            ResultSet bans = Database.select("select * from bans");
            while (bans.next()) {
                Timestamp ts = bans.getTimestamp("expires");
                long expires;
                if (ts != null) {
                    expires = ts.getTime();
                } else {
                    expires = -1;
                }
                banTimer(e.getJDA().getGuildById(mutes.getLong("guildid")), expires, String.valueOf(mutes.getLong("userid")));
            }*/
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (expEdited) {
                        saveLevelJSON();
                    }
                }
            }, 0, 30000);
            //e.getJDA().getTextChannelById("756828765065183253").retrieveMessageById("756828900083892264").queue(helps::add);
            //e.getJDA().getTextChannelById("757170844240707634").retrieveMessageById("757171205055840286").queue(helps::add);
            //e.getJDA().getTextChannelById("715249205186265178").retrieveMessageById("758397956637720596").queue(helps::add);
            Guild g = e.getJDA().getGuildById("712035338846994502");
            TextChannel channel = e.getJDA().getTextChannelById("715249205186265178");
            //channel.retrieveMessageById("758397956637720596").queue(helps::add);
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
        } catch (Exception ex) {
            ex.printStackTrace();
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
        MessageReaction.ReactionEmote reactionEmote = e.getReactionEmote();
        if (e.getMessageId().equals("778380596413464676") && reactionEmote.isEmote()) {
            String eid = reactionEmote.getEmote().getId();
            if (eid.equals("821069953867841578")) {
                updateShinyCounts();
            } else {
                JSONObject counter = shinycountjson.getJSONObject("counter");
                Optional<String> mop = counter.keySet().stream().filter(s -> counter.getJSONObject(s).getString("emote").equals(eid)).findFirst();
                if (mop.isPresent()) {
                    String method = mop.get();
                    counter.getJSONObject(method).put(mem.getId(), counter.getJSONObject(method).optInt(mem.getId(), 0) + 1);
                    e.getJDA().getTextChannelById("778380440078647296").removeReactionById("778380596413464676", reactionEmote.getEmote(), e.getUser()).queue();
                    updateShinyCounts();
                }
            }
        }
        if (e.getMessageId().equals("755331617970454558")) {
            e.getReaction().clearReactions().queue();
        }
        Optional<Message> op = helps.stream().filter(m -> m.getId().equals(e.getMessageId())).findFirst();
        if (op.isPresent()) {
            e.getReaction().removeReaction(e.getUser()).queue();
            Message m = op.get();
            if (reactionEmote.isEmoji()) {
                String emoji = reactionEmote.getEmoji();
                if (emoji.equals("◀️")) {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Commands").setColor(java.awt.Color.CYAN);
                    builder.setDescription(getHelpDescripion(g, mem));
                    builder.setColor(java.awt.Color.CYAN);
                    addReactions(m, mem);
                    m.editMessage(builder.build()).queue();
                }
            }
            CommandCategory.getOrder().stream().filter(cat -> {
                if (cat.allowsMember(mem) && cat.allowsGuild(g)) {
                    if (cat.isEmote() && reactionEmote.isEmote()) {
                        return cat.getEmoji().equals(reactionEmote.getEmote().getId());
                    } else {
                        if (reactionEmote.isEmoji()) {
                            return cat.getEmoji().equals(reactionEmote.getEmoji());
                        }
                    }
                }
                return false;
            }).findFirst().ifPresent(c -> m.editMessage(
                    new EmbedBuilder().setTitle(c.getName()).setColor(java.awt.Color.CYAN).setDescription(getWithCategory(c, g, mem).stream().map(cmd -> cmd.getHelp(g)).collect(Collectors.joining("\n")) + "\n\u25c0\ufe0f Zurück zur Übersicht").build()).queue());

        }
    }

    @Override
    public void onTextChannelCreate(TextChannelCreateEvent e) {
        Guild g = e.getGuild();
        if (g.getId().equals("712035338846994502"))
            e.getChannel().getManager().putPermissionOverride(g.getRoleById("717297533294215258"), null, Collections.singletonList(Permission.MESSAGE_WRITE)).queue();
        else if (g.getId().equals("447357526997073930"))
            e.getChannel().getManager().putPermissionOverride(g.getRoleById("761723664273899580"), null, Collections.singletonList(Permission.MESSAGE_WRITE)).queue();
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent e) {
        Guild g = e.getGuild();
        if (g.getIdLong() != Constants.ASLID) return;
        g.retrieveMember(e.getInvite().getInviter()).queue(mem -> {
            if (g.getSelfMember().canInteract(mem)) e.getInvite().delete().queue();
        });
    }

    @Override
    public void onGuildMessageReceived(@org.jetbrains.annotations.NotNull GuildMessageReceivedEvent e) {
        new Thread(() -> {
            try {
                if (e.isWebhookMessage()) return;
                Message m = e.getMessage();
                String msg = m.getContentDisplay();
                TextChannel tco = e.getChannel();
                Member member = e.getMember();
                Guild g = e.getGuild();
                long gid = g.getIdLong();
                long meid = member.getIdLong();
                if (tco.getIdLong() == 835501361000087612L) {
                    String[] split = msg.split(":");
                    if (split[1].equals("play"))
                        loadJarvisPlaylist(split[0]);
                    else if (split[1].equals("stop")) {
                        getGuildAudioPlayer(e.getJDA().getGuildById(split[0])).player.stopTrack();
                    }
                }
                if (tco.getIdLong() == 857960777851994142L) {
                    ArrayList<File> at = new ArrayList<>();
                    m.getAttachments().forEach(a -> {
                        try {
                            at.add(a.downloadToFile("nmlattachments/" + a.getFileName()).get());
                        } catch (InterruptedException | ExecutionException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    });
                    DBManagers.NML_ANNOUNCEMENTS.forAll(c -> {
                        MessageAction ma = e.getJDA().getTextChannelById(c.getLong("channelid")).sendMessage(m.getContentRaw());
                        //noinspection ResultOfMethodCallIgnored
                        at.forEach(ma::addFile);
                        ma.queue();
                    });
                }
                if (!e.getAuthor().isBot()) {
                    if (tco.getIdLong() == 846889514613735445L || tco.getIdLong() == 849303690808786955L) {
                        for (Message.Attachment attachment : m.getAttachments()) {
                            attachment.downloadToFile("./taria/old/" + attachment.getFileName()).thenAccept(f -> {
                                try {
                                    File out = new File("./taria/new/" + f.getName());
                                    ImageIO.write(resizeImage(f), "png", out);
                                    tco.sendFile(out).queue();
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            });
                        }
                    }
                }
                if (meid == Constants.FLOID) {
                    if (msg.contains("518008523653775366")) gid = 518008523653775366L;
                    if (msg.contains("709877545708945438")) gid = 709877545708945438L;
                    if (msg.contains("747357029714231299")) gid = 747357029714231299L;
                    if (msg.contains("736555250118295622")) gid = 736555250118295622L;
                }
                if (gid == 712035338846994502L) {
                    if (g.getSelfMember().canInteract(member) && !allowsCaps.contains(tco.getId())) {
                        int x = 0;
                        for (char c : msg.toCharArray()) {
                            if (c >= 65 && c <= 90) x++;
                        }
                        if (msg.length() > 3 && (double) x / (double) msg.length() > 0.6) {
                            m.delete().queue();
                            warn(tco, g.getSelfMember(), member, "Capslock\nNachricht: " + msg);
                        }
                    }
                    if (System.currentTimeMillis() - latestExp.getOrDefault(meid, (long) 0) > 60000 && !member.getUser().isBot()) {
                        latestExp.put(meid, System.currentTimeMillis());
                        JSONObject levelsystem = getLevelJSON();
                        int exp = levelsystem.optInt(String.valueOf(meid), 0);
                        int oldlevel = getLevelFromXP(exp);
                        exp += (new Random().nextInt(10) + 15) * expmultiplicator.getOrDefault(meid, (double) 1);
                        int newlevel = getLevelFromXP(exp);
                        levelsystem.put(String.valueOf(meid), exp);
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
                    if (gameday == -1) return;
                    String p1 = member.getId();
                    Optional<String> op = Arrays.stream(json.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";")).filter(str -> str.contains(p1)).findFirst();
                    if (op.isEmpty()) {
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
                            EmolgaMain.emolgajda.getGuildById(Constants.BSID).retrieveMembersByIds(names.toArray(new String[0])).get().forEach(mem -> namesmap.put(mem.getId(), mem.getEffectiveName()));
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
            /*if (tco.getId().equals("759712094223728650") || tco.getId().equals("759734608773775360")) {
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
                            .addAll("Playoffs!" + getAsXCoord((gameday - 8) * 7 - 4) + yy, r1)
                            .addAll("Playoffs!" + getAsXCoord((gameday - 8) * 7 - 2) + yy, r2);
                } else {
                    b
                            .addAll("Playoffs!" + getAsXCoord((gameday - 8) * 7 - 4) + yy, r2)
                            .addAll("Playoffs!" + getAsXCoord((gameday - 8) * 7 - 2) + yy, r1);
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
            }*/
                if (tco.getId().equals("743471003220443226") && !member.getUser().isBot()) {
                    e.getJDA().retrieveUserById("574949229668335636").complete().openPrivateChannel().complete().sendMessage(msg).queue();
                    return;
                }
                check(e);
                if (gid == 447357526997073930L) {
                    PrivateCommands.execute(e.getMessage());
                }
                if (tco.getId().equals("758198459563114516")) {
                    g.addRoleToMember(member, g.getRoleById("758254829885456404")).queue();
                }
                if (m.getMentionedMembers().size() == 1) {
                    if (e.getJDA().getSelfUser().getIdLong() == m.getMentionedMembers().get(0).getIdLong() && !e.getAuthor().isBot() && isChannelAllowed(tco)) {
                        help(tco, member);
                    }
                }
                if ((tco.getId().equals("712612442622001162") || tco.getId().equals("724034089891397692")) && m.getAttachments().size() > 0) {
                    tco.sendMessage("Gz!").queue();
                }
                if (emoteSteal.contains(tco.getIdLong())) {
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
                if (meid == 159985870458322944L && g.getIdLong() == 712035338846994502L) {
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
                                case 5 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248666008354847")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715242519528603708")).queue();
                                }
                                case 10 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715247650018164826")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715248666008354847")).queue();
                                }
                                case 15 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248788314259546")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715247650018164826")).queue();
                                }
                                case 20 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248194732163163")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715248788314259546")).queue();
                                }
                                case 30 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248297471770640")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715248194732163163")).queue();
                                }
                                case 35 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248393223667753")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715248297471770640")).queue();
                                }
                                case 40 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715247811687612547")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715248393223667753")).queue();
                                }
                                case 50 -> {
                                    g.addRoleToMember(mem, g.getRoleById("715248587344183347")).queue();
                                    g.removeRoleFromMember(mem, g.getRoleById("715247811687612547")).queue();
                                }
                            }
                        } catch (Exception ex) {
                            e.getJDA().retrieveUserById(Constants.FLOID).complete().openPrivateChannel().complete().sendMessage("Fehler bei Level Up!").queue();
                        }
                    }
                    return;
                }
                if (!e.getAuthor().isBot() && !msg.startsWith("!dexquiz")) {
                    DexQuiz quiz = DexQuiz.getByTC(tco);
                    if (quiz != null && !quiz.block) {
                        Translation name = getGerName(msg);
                        if (quiz.check(name)) {
                            quiz.block = true;
                            tco.sendMessage(member.getAsMention() + " hat das Pokemon erraten! Es war **" + quiz.gerName + "**! (Der Eintrag stammt aus **Pokemon " + quiz.edition + "**)").queue();
                            quiz.round++;
                            if (!quiz.points.containsKey(member)) quiz.points.put(member, 0);
                            quiz.points.put(member, quiz.points.get(member) + 1);
                            if (quiz.round > quiz.cr) {
                                quiz.end();
                                return;
                            }
                            quiz.newMon();
                        }
                    }
                }
                JSONObject json = getEmolgaJSON();
                if (replayAnalysis.containsKey(tco.getIdLong()) && !e.getAuthor().getId().equals(e.getJDA().getSelfUser().getId()) && !msg.contains("!analyse ")) {
                    TextChannel t = tco.getGuild().getTextChannelById(replayAnalysis.get(tco.getIdLong()));
                    //t.sendTyping().queue();
                    if (msg.contains("https://") || msg.contains("http://")) {
                        Optional<String> urlop = Arrays.stream(msg.split("\n")).filter(s -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/")).map(s -> s.substring(s.indexOf("http"), s.indexOf(" ", s.indexOf("http") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("http") + 1))).findFirst();
                        if (urlop.isPresent()) {
                            String url = urlop.get();
                            System.out.println(url);
                            Player[] game;
                            try {
                                game = Analysis.analyse(url, m);
                            } catch (Exception ex) {
                                tco.getGuild().getTextChannelById(replayAnalysis.get(tco.getIdLong())).sendMessage("Beim Auswerten des Replays ist (vermutlich wegen eines Zoruas/Zoroarks) ein Fehler aufgetreten! Bitte trage das Ergebnis selbst ein!").queue();
                                ex.printStackTrace();
                                return;
                            }
                            System.out.println("Analysed!");
                            System.out.println(g.getName() + " -> " + tco.getAsMention());
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
                            String u1 = game[0].getNickname();
                            String u2 = game[1].getNickname();
                            String name1;
                            String name2;
                            String uid1 = null;
                            String uid2 = null;
                            if (json.getJSONObject("showdown").has(String.valueOf(gid))) {
                                JSONObject showdown = json.getJSONObject("showdown").getJSONObject(String.valueOf(gid));
                                System.out.println("u1 = " + u1);
                                System.out.println("u2 = " + u2);
                                for (String s : showdown.keySet()) {
                                    if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                                    if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
                                }
                                name1 = uid1 != null && gid == 518008523653775366L ? uid1.equals("LSD") ? "REPLACELSD" : e.getJDA().getGuildById(gid).retrieveMemberById(uid1).complete().getEffectiveName() : game[0].getNickname();
                                name2 = uid2 != null && gid == 518008523653775366L ? uid2.equals("LSD") ? "REPLACELSD" : e.getJDA().getGuildById(gid).retrieveMemberById(uid2).complete().getEffectiveName() : game[1].getNickname();
                            } else {
                                name1 = game[0].getNickname();
                                name2 = game[1].getNickname();
                            }
                            System.out.println("uid1 = " + uid1);
                            System.out.println("uid2 = " + uid2);
                            String str;
                            if (spoiler) {
                                str = name1 + " ||" + winloose + "|| " + name2 + "\n\n" + name1 + ":\n" + t1
                                        + "\n" + name2 + ": " + "\n" + t2;
                            } else {
                                str = name1 + " " + winloose + " " + name2 + "\n\n" + name1 + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1
                                        + "\n" + name2 + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2;
                            }
                            t.sendMessage(str).queue();
                            Database.incrementStatistic("analysis");
                            for (int i = 0; i < 2; i++) {
                                if (game[i].getMons().stream().anyMatch(mon -> mon.getPokemon().equals("Zoroark") || mon.getPokemon().equals("Zorua")))
                                    t.sendMessage("Im Team von " + game[i].getNickname() + " befindet sich ein Zorua/Zoroark! Bitte noch einmal die Kills überprüfen!").queue();
                            }
                            System.out.println("In Emolga Listener!");
                            if (gid != 518008523653775366L && gid != 447357526997073930L && gid != 709877545708945438L && gid != 736555250118295622L)
                                return;
                            if (!json.getJSONObject("showdown").has(String.valueOf(gid))) return;
                            if (uid1 == null || uid2 == null) return;
                            if (sdAnalyser.containsKey(gid)) {
                                sdAnalyser.get(gid).analyse(game, uid1, uid2, kills, deaths, new ArrayList[]{p1mons, p2mons}, url);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public boolean isAdmin(Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR) || member.getId().equals("598199247124299776");
    }


}
