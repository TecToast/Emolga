package de.tectoast.emolga.bot;

import de.tectoast.emolga.buttons.ButtonListener;
import de.tectoast.emolga.commands.PrivateCommand;
import de.tectoast.emolga.commands.PrivateCommands;
import de.tectoast.emolga.selectmenus.MenuListener;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private static final Logger logger = LoggerFactory.getLogger(EmolgaListener.class);

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
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent e) {
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

    @SuppressWarnings("HttpUrlsUsage")
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
                analyseReplay(url, null, e.getJDA().getTextChannelById(820359155612254258L), e.getMessage(), null);
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent e) {
        logger.info(e.getOldName() + " -> " + e.getNewName());
        if (e.getUser().getMutualGuilds().stream().map(ISnowflake::getId).toList().contains("518008523653775366"))
            e.getJDA().getTextChannelById("728675253924003870").sendMessage(e.getOldName() + " hat sich auf ganz Discord in " + e.getNewName() + " umbenannt!").queue();
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

    @SuppressWarnings("HttpUrlsUsage")
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        if (e.getGuild().getIdLong() == MYSERVER) {
            logger.info("GOT IT " + System.currentTimeMillis());
        }
        try {
            if (e.isWebhookMessage()) return;
            Message m = e.getMessage();
            String msg = m.getContentDisplay();
            TextChannel tco = e.getChannel();
            Member member = e.getMember();
            Guild g = e.getGuild();
            long gid = g.getIdLong();

            if (msg.toLowerCase().contains("ich bin") && !e.getMessage().getAuthor().isBot() && Arrays.asList(895689011773972490L, 854624056175493161L, 920364843805061221L, 673833176036147210L, 730487435753947136L, 700504340368064562L, 688803880213282889L, 562276305647829022L, 688085496064507954L).contains(gid)) {

                String[] message_split = msg.split(" ");
                int position_name = 0;

                for (int i = 0; i < message_split.length; i++) { //position des bins rausfinden
                    if (message_split[i].equals("bin")) {
                        position_name = i;
                    }
                }
                if (msg.contains(",")) {
                    String name = msg.split(",")[0].split(" ")[position_name + 1];
                    e.getChannel().sendMessage("Hallo " + name + " ich bin Dad").queue();

                } else {
                    if ((position_name + 1) <= message_split.length) {
                        e.getChannel().sendMessage("Hallo " + message_split[position_name + 1] + " ich bin Dad ").queue();
                    }

                }
            }

            check(e);
            if (gid == 447357526997073930L) {
                PrivateCommands.execute(e.getMessage());
            }
            if (tco.getIdLong() == 929841771276554260L) {
                g.addRoleToMember(member, g.getRoleById(934810601216147477L)).queue();
            }
            String raw = m.getContentRaw();
            long id = e.getJDA().getSelfUser().getIdLong();
            if (raw.equals("<@!" + id + ">") || raw.equals("<@" + id + ">") && !e.getAuthor().isBot() && isChannelAllowed(tco)) {
                help(tco, member);
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
                    String mid = member.getId().equals("893773494578470922") ? "598199247124299776" : member.getId();
                    if (msg.contains("!set ")) {
                        o.put(mid, Integer.parseInt(split[2]));
                    } else if (msg.contains("!reset ")) {
                        o.put(mid, 0);
                    } else if (msg.contains("!add ")) {
                        o.put(mid, o.optInt(mid, 0) + Integer.parseInt(split[2]));
                    } else isCmd = false;
                    if (isCmd) {
                        m.delete().queue();
                        updateShinyCounts(tco.getIdLong());
                    }
                }
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
                        logger.info(url);
                        analyseReplay(url, null, t, m, null);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
