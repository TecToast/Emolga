package de.tectoast.emolga.bot

import de.tectoast.emolga.buttons.ButtonListener.Companion.check
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.commands.music.custom.WirklichGuteMusikCommand.Companion.doIt
import de.tectoast.emolga.modals.ModalListener
import de.tectoast.emolga.selectmenus.MenuListener
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.DASORID
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.MYSERVER
import de.tectoast.emolga.utils.DexQuiz
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.emolga.utils.sql.managers.BanManager
import de.tectoast.emolga.utils.sql.managers.MuteManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class EmolgaListener : ListenerAdapter() {
    override fun onButtonInteraction(e: ButtonInteractionEvent) {
        check(e)
    }

    override fun onModalInteraction(e: ModalInteractionEvent) {
        //e.reply(e.getValues().stream().map(m -> "%s: %s (%s)".formatted(m.getId(), m.getAsString(), m.getType().toString())).collect(Collectors.joining("\n"))).queue();
        ModalListener.check(e)
    }

    override fun onSelectMenuInteraction(e: SelectMenuInteractionEvent) {
        MenuListener.check(e)
    }

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        val g = e.guild
        val mem = e.member
        val gid = g.idLong
        if (gid == Constants.BSID) {
            g.addRoleToMember(mem, g.getRoleById(715242519528603708L)!!).queue()
        }
        if (gid == Constants.ASLID) {
            g.getTextChannelById(615605820381593610L)!!.sendMessage(
                """
                    Willkommen auf der ASL, ${mem.asMention}. <:hi:540969951608045578>
                    Dies ist ein Pokémon Server mit dem Fokus auf einem kompetetiven Draftligasystem. Mach dich mit dem <#635765395038666762> vertraut und beachte die vorgegebenen Themen der Kanäle. Viel Spaß! <:yay:540970044297838597>
                    """.trimIndent()
            ).queue()
        }
    }

    override fun onGuildVoiceMove(e: GuildVoiceMoveEvent) {
        if (e.channelLeft.members.size == 1 && e.guild.audioManager.isConnected) {
            e.guild.audioManager.closeAudioConnection()
        }
    }

    override fun onGuildVoiceJoin(e: GuildVoiceJoinEvent) {
        if (e.channelJoined.idLong == 979436321359683594L) {
            val mid = e.member.idLong
            if (mid == e.jda.selfUser.idLong) return
            doIt(e.guild.getTextChannelById(861558360104632330L)!!, e.member, mid == FLOID || mid == DASORID)
        }
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        val g = e.guild
        g.retrieveOwner().flatMap { m: Member -> m.user.openPrivateChannel() }
            .queue { pc: PrivateChannel ->
                pc.sendMessage(
                    WELCOMEMESSAGE.replace(
                        "{USERNAME}", g.owner!!
                            .user.name
                    ).replace("{SERVERNAME}", g.name)
                ).queue()
            }
        e.jda.retrieveUserById(FLOID).flatMap { obj: User -> obj.openPrivateChannel() }
            .flatMap { u: PrivateChannel ->
                u.sendMessage("${g.name} (${g.id})").setActionRow(
                    Button.primary("guildinvite;" + g.id, "Invite").withEmoji(
                        Emoji.fromUnicode("✉️")
                    )
                )
            }
            .queue()
    }

    override fun onRoleCreate(e: RoleCreateEvent) {
        if (e.guild.id != "736555250118295622" && e.guild.id != "447357526997073930" && e.guild.id != "518008523653775366") return
        e.role.manager.revokePermissions(Permission.CREATE_INSTANT_INVITE).queue()
    }

    override fun onMessageReceived(e: MessageReceivedEvent) {
        if (e.channelType == ChannelType.TEXT) {
            try {
                if (e.isWebhookMessage) return
                val m = e.message
                val msg = m.contentDisplay
                val tco = e.textChannel
                val member = e.member
                val g = e.guild
                val gid = g.idLong
                Command.check(e)
                if (gid == MYSERVER) {
                    PrivateCommands.execute(e.message)
                    if (tco.parentCategoryIdLong == EMOLGA_KI) {
                        val split = tco.name.split("-".toRegex())
                        e.jda.getTextChannelById(split[split.size - 1])!!.sendMessage(m.contentRaw).queue()
                    }
                }
                if (tco.idLong == 929841771276554260L) {
                    g.addRoleToMember(member!!, g.getRoleById(934810601216147477L)!!).queue()
                }
                val raw = m.contentRaw
                val id = e.jda.selfUser.idLong
                if (raw == "<@!$id>" || raw == "<@$id>" && !e.author.isBot && Command.isChannelAllowed(tco)) {
                    Command.help(tco, member)
                }
                if (Command.emoteSteal.contains(tco.idLong)) {
                    val l = m.mentions.emotes
                    for (emote in l) {
                        try {
                            g.createEmote(emote.name, Icon.from(URL(emote.imageUrl).openStream())).queue()
                        } catch (ioException: IOException) {
                            ioException.printStackTrace()
                        }
                    }
                }
                if (tco.idLong == 778380440078647296L || tco.idLong == 919641011632881695L) {
                    val split = msg.split(" ".toRegex())
                    val counter = Command.shinycountjson.getJSONObject("counter")
                    val mop = counter.keySet().stream().filter { s: String ->
                        s.lowercase(Locale.getDefault()).startsWith(
                            split[1].lowercase(Locale.getDefault())
                        )
                    }.findFirst()
                    if (mop.isPresent) {
                        val o = Command.shinycountjson.getJSONObject("counter").getJSONObject(mop.get())
                        var isCmd = true
                        val mid = if (member!!.id == "893773494578470922") "598199247124299776" else member.id
                        if (msg.contains("!set ")) {
                            o.put(mid, split[2].toInt())
                        } else if (msg.contains("!reset ")) {
                            o.put(mid, 0)
                        } else if (msg.contains("!add ")) {
                            o.put(mid, o.optInt(mid, 0) + split[2].toInt())
                        } else isCmd = false
                        if (isCmd) {
                            m.delete().queue()
                            Command.updateShinyCounts(tco.idLong)
                        }
                    }
                }
                if (!e.author.isBot && !msg.startsWith("!dexquiz")) {
                    val quiz = DexQuiz.getByTC(tco)
                    if (quiz != null && quiz.nonBlocking()) {
                        if (quiz.check(msg)) {
                            quiz.block()
                            tco.sendMessage(member!!.asMention + " hat das Pokemon erraten! Es war **" + quiz.currentGerName + "**! (Der Eintrag stammt aus **Pokemon " + quiz.currentEdition + "**)")
                                .queue()
                            quiz.givePoint(member.idLong)
                            quiz.nextRound()
                        }
                    }
                }
                if (Command.replayAnalysis.containsKey(tco.idLong) && e.author.id != e.jda.selfUser.id && !msg.contains(
                        "!analyse "
                    ) && !msg.contains("!sets ")
                ) {
                    val t = tco.guild.getTextChannelById(Command.replayAnalysis[tco.idLong]!!)!!
                    //t.sendTyping().queue();
                    if (msg.contains("https://") || msg.contains("http://")) {
                        val urlop = Arrays.stream(msg.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray())
                            .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                            .map { s: String ->
                                s.substring(
                                    s.indexOf("http"),
                                    if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                        ' ',
                                        s.indexOf("http") + 1
                                    )
                                )
                            }
                            .findFirst()
                        if (urlop.isPresent) {
                            val url = urlop.get()
                            logger.info(url)
                            Command.analyseReplay(url, null, t, m, null)
                        }
                    }
                }
            } catch (ex: IllegalStateException) {
                Command.sendToMe(e.channelType.name + " Illegal Argument Exception Channel")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else if (e.isFromType(ChannelType.PRIVATE)) {
            if (!e.author.isBot && e.author.idLong != FLOID) e.jda.getTextChannelById(828044461379682314L)!!
                .sendMessage(e.author.asMention + ": " + e.message.contentDisplay).queue()
            if (e.author.idLong == FLOID) {
                PrivateCommands.execute(e.message)
            }
            PrivateCommand.check(e)
            val msg = e.message.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                val urlop = msg.split("\n".toRegex()).asSequence()
                    .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                    .map { s: String ->
                        s.substring(
                            s.indexOf("http"),
                            if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                ' ',
                                s.indexOf("http") + 1
                            )
                        )
                    }
                    .firstOrNull()
                urlop?.run {
                    Command.analyseReplay(this, null, e.jda.getTextChannelById(820359155612254258L)!!, e.message, null)
                }
            }
        }
    }

    override fun onGuildVoiceLeave(e: GuildVoiceLeaveEvent) {
        if (e.member.id == "723829878755164202") {
            val manager = Command.getGuildAudioPlayer(e.guild)
            manager.scheduler.queue.clear()
            manager.scheduler.nextTrack()
        }
        if (e.channelLeft.members.size == 1 && e.guild.audioManager.isConnected) {
            e.guild.audioManager.closeAudioConnection()
        }
    }

    override fun onReady(e: ReadyEvent) {
        try {
            Command.uninitializedCommands.forEach { Command.sendToMe("No Argument Manager Template: $it") }
            val jda = e.jda
            if (jda.selfUser.idLong == 723829878755164202L) {
                Draft.init()
                BanManager.forAll { rs: ResultSet ->
                    jda.getGuildById(rs.getLong("guildid"))?.run {
                        Command.banTimer(
                            this,
                            Optional.ofNullable(rs.getTimestamp("expires")).map { obj: Timestamp -> obj.time }
                                .orElse(-1L),
                            rs.getLong("userid"))
                    }
                }
                MuteManager.forAll { rs: ResultSet ->
                    jda.getGuildById(rs.getLong("guildid"))?.run {
                        Command.muteTimer(
                            this,
                            Optional.ofNullable(rs.getTimestamp("expires")).map { obj: Timestamp -> obj.time }
                                .orElse(-1L),
                            rs.getLong("userid"))
                    }
                }
                //new Draft(jda.getTextChannelById(837425828245667841L), "NDS", null, true, true);
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onUserUpdateName(e: UserUpdateNameEvent) {
        logger.info(e.oldName + " -> " + e.newName)
        if (e.user.mutualGuilds.stream().map { obj: Guild -> obj.idLong }
                .anyMatch { l: Long -> l == Constants.ASLID }) e.jda.getTextChannelById("728675253924003870")!!
            .sendMessage(e.oldName + " hat sich auf ganz Discord in " + e.newName + " umbenannt!").queue()
    }

    override fun onChannelCreate(e: ChannelCreateEvent) {
        if (e.channelType.isGuild) {
            val g = e.guild
            val channel = e.channel as GuildChannel
            if (g.id == "447357526997073930") channel.permissionContainer.upsertPermissionOverride(g.getRoleById("761723664273899580")!!)
                .setDenied(
                    Permission.MESSAGE_SEND
                ).queue()
        }
    }

    override fun onGuildInviteCreate(e: GuildInviteCreateEvent) {
        val g = e.guild
        if (g.idLong != Constants.ASLID) return
        g.retrieveMember(e.invite.inviter!!).queue { mem: Member? ->
            if (g.selfMember.canInteract(
                    mem!!
                )
            ) e.invite.delete().queue()
        }
    }

    override fun onCommandAutoCompleteInteraction(e: CommandAutoCompleteInteractionEvent) {
        if (e.commandType == net.dv8tion.jda.api.interactions.commands.Command.Type.SLASH) {
            val focusedOption = e.focusedOption
            val arg = Command.byName(e.name)!!.argumentTemplate.find(focusedOption.name)!!
            val type = arg.type
            if (type.hasAutoComplete()) {
                type.autoCompleteList(focusedOption.value).run {
                    e.replyChoiceStrings(this ?: emptyList()).queue()
                }
            }
        }
    }

    companion object {
        val WELCOMEMESSAGE: String = """
            Hallo **{USERNAME}** und vielen Dank, dass du mich auf deinen Server **{SERVERNAME}** geholt hast!
            Vermutlich möchtest du für deinen Server hauptsächlich, dass die Ergebnisse von Showdown Replays in einen Channel geschickt werden.
            Um mich zu konfigurieren gibt es folgende Möglichkeiten:

            **1. Die Ergebnisse sollen in den gleichen Channel geschickt werden:**
            Einfach `!replaychannel` in den jeweiligen Channel schreiben

            **2. Die Ergebnisse sollen in einen anderen Channel geschickt werden:**
            `!replaychannel #Ergebnischannel` in den Channel schicken, wo später die Replays reingeschickt werden sollen (Der #Ergebnischannel ist logischerweise der Channel, wo später die Ergebnisse reingeschickt werden sollen)

            Falls die Ergebnisse in ||Spoilertags|| geschickt werden sollen, schick irgendwo auf dem Server den Command `!spoilertags` rein. Dies gilt dann serverweit.

            Ich habe übrigens noch viele weitere Funktionen! Wenn du mich pingst, zeige ich dir eine Übersicht aller Commands :)
            Falls du weitere Fragen oder Probleme hast, schreibe ${Constants.MYTAG} eine PN :)
        """.trimIndent()
        private val logger = LoggerFactory.getLogger(EmolgaListener::class.java)
    }
}