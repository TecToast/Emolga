package de.tectoast.emolga.bot;

import de.tectoast.emolga.buttons.ButtonListener;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.PrivateCommand;
import de.tectoast.emolga.commands.PrivateCommands;
import de.tectoast.emolga.selectmenus.MenuListener;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.music.GuildMusicManager;
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
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

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
import static de.tectoast.emolga.utils.Constants.MYSERVER;

public class EmolgaListener extends ListenerAdapter {
    public static final List<String> allowsCaps = Arrays.asList("712612442622001162", "752230819644440708", "732545253344804914");
    public static final String WELCOMEMESSAGE = """
            Hallo **{USERNAME}** und vielen Dank, dass du mich auf deinen Server **{SERVERNAME}** geholt hast!
            Vermutlich möchtest du für deinen Server hauptsächlich, dass die Ergebnisse von Showdown Replays in einen Channel geschickt werden.
            Um mich zu konfigurieren gibt es folgende Möglichkeiten:

            **1. Die Ergebnisse sollen in den gleichen Channel geschickt werden:**
            Einfach `!replaychannel` in den jeweiligen Channel schreiben

            **2. Die Ergebnisse sollen in einen anderen Channel geschickt werden:**
            `!replaychannel #Ergebnischannel` in den Channel schicken, wo später die Replays reingeschickt werden sollen (Der #Ergebnischannel ist logischerweise der Channel, wo später die Ergebnisse reingeschickt werden sollen)

            Falls die Ergebnisse in ||Spoilertags|| geschickt werden sollen, schick irgendwo auf dem Server den Command `!spoilertags` rein. Dies gilt dann serverweit.

            Ich habe übrigens noch viele weitere Funktionen! Wenn du mich pingst, zeige ich dir eine Übersicht aller Commands :)
            Falls du weitere Fragen oder Probleme hast, schreibe Flooo#2535 eine PN :)""";
    public static boolean disablesort = false;

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
    public void onSelectionMenu(@NotNull SelectionMenuEvent e) {
        MenuListener.check(e);
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
        //if (e.getMember().getId().equals("634093507388243978")) e.getGuild().kickVoiceMember(e.getMember()).queue();
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent e) {
        if(permaMoveGugsi && e.getChannelLeft().getIdLong() == 887432239825190912L && e.getMember().getIdLong() == 694543579414134802L) {
            e.getGuild().moveVoiceMember(e.getMember(), e.getChannelLeft()).queue();
        }
        if (e.getChannelLeft().getMembers().size() == 1 && e.getGuild().getAudioManager().isConnected()) {
            e.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent e) {
        e.getGuild().retrieveOwner().flatMap(m -> m.getUser().openPrivateChannel()).queue(pc -> pc.sendMessage(WELCOMEMESSAGE.replace("{USERNAME}", e.getGuild().getOwner().getUser().getName()).replace("{SERVERNAME}", e.getGuild().getName())).queue());
        sendToMe(e.getGuild().getTextChannels().get(0).createInvite().setMaxUses(1).complete().getUrl());
    }

    @Override
    public void onRoleCreate(@Nonnull RoleCreateEvent e) {
        if (!e.getGuild().getId().equals("736555250118295622") && !e.getGuild().getId().equals("447357526997073930") && !e.getGuild().getId().equals("518008523653775366"))
            return;
        e.getRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE).queue();
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent e) {
        if (!e.getAuthor().isBot() && e.getAuthor().getIdLong() != Constants.FLOID)
            e.getJDA().getTextChannelById(828044461379682314L).sendMessage(e.getAuthor().getAsMention() + ": " + e.getMessage().getContentDisplay()).queue();
        if (e.getAuthor().getIdLong() == Constants.FLOID) {
            PrivateCommands.execute(e.getMessage());
        }
        PrivateCommand.check(e);
        String msg = e.getMessage().getContentDisplay();
        long gid = Constants.ASLID;
        Guild g = e.getJDA().getGuildById(gid);
        if (msg.contains("https://") || msg.contains("http://")) {
            Optional<String> urlop = Arrays.stream(msg.split("\n")).filter(s -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/")).map(s -> s.substring(s.indexOf("http"), s.indexOf(" ", s.indexOf("http") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("http") + 1))).findFirst();
            if (urlop.isPresent()) {
                String url = urlop.get();
                analyseReplay(url, e.getJDA().getTextChannelById(882641809531101256L), e.getJDA().getTextChannelById(882642106533949451L), e.getMessage(), null);
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
        if (e.getChannelLeft().getMembers().size() == 1 && e.getGuild().getAudioManager().isConnected()) {
            e.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        try {
            if (e.getJDA().getSelfUser().getIdLong() == 723829878755164202L)
                Draft.init(e.getJDA());
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
        /*if (e.getMessageId().equals("778380596413464676") && reactionEmote.isEmote()) {
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
        }*/
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
                    m.editMessageEmbeds(builder.build()).queue();
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
            }).findFirst().ifPresent(c -> m.editMessageEmbeds(
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
        if(e.getGuild().getIdLong() == MYSERVER) {
            System.out.println("GOT IT " + System.currentTimeMillis());
        }
        //new Thread(() -> {
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
                    if (msg.contains("837425304896536596")) gid = 837425304896536596L;
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
                String raw = m.getContentRaw();
                long id = e.getJDA().getSelfUser().getIdLong();
                if(raw.equals("<@!" + id + ">") || raw.equals("<@" + id + ">") && !e.getAuthor().isBot() && isChannelAllowed(tco)) {
                    help(tco, member);
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
                if (tco.getIdLong() == 778380440078647296L || tco.getIdLong() == 919641011632881695L) {
                    String[] split = msg.split(" ");
                    JSONObject counter = shinycountjson.getJSONObject("counter");
                    Optional<String> mop = counter.keySet().stream().filter(s -> s.toLowerCase().startsWith(split[1].toLowerCase())).findFirst();
                    if (mop.isPresent()) {
                        JSONObject o = shinycountjson.getJSONObject("counter").getJSONObject(mop.get());
                        boolean isCmd = true;
                        if (msg.contains("!set ")) {
                            o.put(member.getId(), Integer.parseInt(split[2]));
                        } else if (msg.contains("!reset ")) {
                            o.put(member.getId(), 0);
                        } else if (msg.contains("!add ")) {
                            o.put(member.getId(), o.optInt(member.getId(), 0) + Integer.parseInt(split[2]));
                        } else isCmd = false;
                        if (isCmd) {
                            m.delete().queue();
                            updateShinyCounts(tco.getIdLong());
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
                        if (quiz.check(msg)) {
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
                            analyseReplay(url, null, t, m, null);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        //}).start();
    }
}
