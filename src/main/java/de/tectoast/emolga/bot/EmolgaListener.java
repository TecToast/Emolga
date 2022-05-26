package de.tectoast.emolga.bot;

import de.tectoast.emolga.buttons.ButtonListener;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.PrivateCommand;
import de.tectoast.emolga.commands.PrivateCommands;
import de.tectoast.emolga.selectmenus.MenuListener;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.utils.Constants.EMOLGA_KI;
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
            Falls du weitere Fragen oder Probleme hast, schreibe %s eine PN :)""".formatted(Constants.MYTAG);
    private static final Logger logger = LoggerFactory.getLogger(EmolgaListener.class);

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        ButtonListener.check(e);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent e) {
        e.reply(e.getValues().stream().map(m -> "%s: %s (%s)".formatted(m.getId(), m.getAsString(), m.getType().toString())).collect(Collectors.joining("\n"))).queue();
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent e) {
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
        Guild g = e.getGuild();
        g.retrieveOwner().flatMap(m -> m.getUser().openPrivateChannel()).queue(pc -> pc.sendMessage(WELCOMEMESSAGE.replace("{USERNAME}", g.getOwner().getUser().getName()).replace("{SERVERNAME}", g.getName())).queue());
        e.getJDA().retrieveUserById(Constants.FLOID).flatMap(User::openPrivateChannel).flatMap(
                        u -> u.sendMessage("%s (%s)".formatted(g.getName(), g.getId())).setActionRow(Button.primary("guildinvite;" + g.getId(), "Invite").withEmoji(Emoji.fromUnicode("✉️"))))
                .queue();
    }

    @Override
    public void onRoleCreate(@Nonnull RoleCreateEvent e) {
        if (!e.getGuild().getId().equals("736555250118295622") && !e.getGuild().getId().equals("447357526997073930") && !e.getGuild().getId().equals("518008523653775366"))
            return;
        e.getRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE).queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.getChannelType().isGuild()) {
            try {
                if (e.isWebhookMessage()) return;
                Message m = e.getMessage();
                String msg = m.getContentDisplay();
                TextChannel tco = e.getTextChannel();
                Member member = e.getMember();
                Guild g = e.getGuild();
                long gid = g.getIdLong();
                check(e);
                if (gid == MYSERVER) {
                    PrivateCommands.execute(e.getMessage());
                    if (tco.getParentCategoryIdLong() == EMOLGA_KI) {
                        String[] split = tco.getName().split("-");
                        e.getJDA().getTextChannelById(split[split.length - 1]).sendMessage(m.getContentRaw()).queue();
                    }
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
                    List<Emote> l = m.getMentions().getEmotes();
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
                    if (quiz != null && quiz.nonBlocking()) {
                        if (quiz.check(msg)) {
                            quiz.block();
                            tco.sendMessage(member.getAsMention() + " hat das Pokemon erraten! Es war **" + quiz.getCurrentGerName() + "**! (Der Eintrag stammt aus **Pokemon " + quiz.getCurrentEdition() + "**)").queue();
                            quiz.givePoint(member.getIdLong());
                            quiz.nextRound();
                        }
                    }
                }
                JSONObject json = getEmolgaJSON();
                if (replayAnalysis.containsKey(tco.getIdLong()) && !e.getAuthor().getId().equals(e.getJDA().getSelfUser().getId()) && !msg.contains("!analyse ") && !msg.contains("!sets ")) {
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
            } catch (IllegalStateException ex) {
                sendToMe(e.getChannelType().name() + " Illegal Argument Exception Channel");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.isFromType(ChannelType.PRIVATE)) {
            if (!e.getAuthor().isBot() && e.getAuthor().getIdLong() != Constants.FLOID)
                e.getJDA().getTextChannelById(828044461379682314L).sendMessage(e.getAuthor().getAsMention() + ": " + e.getMessage().getContentDisplay()).queue();
            if (e.getAuthor().getIdLong() == Constants.FLOID) {
                PrivateCommands.execute(e.getMessage());
            }
            PrivateCommand.check(e);
            String msg = e.getMessage().getContentDisplay();
            if (msg.contains("https://") || msg.contains("http://")) {
                Optional<String> urlop = Arrays.stream(msg.split("\n")).filter(s -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/")).map(s -> s.substring(s.indexOf("http"), s.indexOf(" ", s.indexOf("http") + 1) == -1 ? s.length() : s.indexOf(" ", s.indexOf("http") + 1))).findFirst();
                if (urlop.isPresent()) {
                    String url = urlop.get();
                    analyseReplay(url, null, e.getJDA().getTextChannelById(820359155612254258L), e.getMessage(), null);
                }
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
            JDA jda = e.getJDA();
            if (jda.getSelfUser().getIdLong() == 723829878755164202L) {
                Draft.init(jda);
                DBManagers.BAN.forAll(rs -> Command.banTimer(jda.getGuildById(rs.getLong("guildid")), Optional.ofNullable(rs.getTimestamp("expires")).map(Timestamp::getTime).orElse(-1L), rs.getLong("userid")));
                DBManagers.MUTE.forAll(rs -> Command.muteTimer(jda.getGuildById(rs.getLong("guildid")), Optional.ofNullable(rs.getTimestamp("expires")).map(Timestamp::getTime).orElse(-1L), rs.getLong("userid")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent e) {
        logger.info(e.getOldName() + " -> " + e.getNewName());
        if (e.getUser().getMutualGuilds().stream().map(ISnowflake::getIdLong).anyMatch(l -> l == Constants.ASLID))
            e.getJDA().getTextChannelById("728675253924003870").sendMessage(e.getOldName() + " hat sich auf ganz Discord in " + e.getNewName() + " umbenannt!").queue();
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent e) {
        if (e.getChannelType().isGuild()) {
            Guild g = e.getGuild();
            GuildChannel channel = (GuildChannel) e.getChannel();
            if (g.getId().equals("447357526997073930"))
                channel.getPermissionContainer().upsertPermissionOverride(g.getRoleById("761723664273899580")).setDenied(Permission.MESSAGE_SEND).queue();
        }
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
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent e) {
        if (e.getCommandType() == net.dv8tion.jda.api.interactions.commands.Command.Type.SLASH) {
            AutoCompleteQuery focusedOption = e.getFocusedOption();
            ArgumentManagerTemplate.Argument arg = byName(e.getName()).getArgumentTemplate().find(focusedOption.getName());
            ArgumentType type = arg.getType();
            if (type.hasAutoComplete()) {
                e.replyChoiceStrings(type.autoCompleteList(focusedOption.getValue())).queue();
            }
        }
    }
}
