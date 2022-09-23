package de.tectoast.emolga.bot

import de.tectoast.emolga.buttons.ButtonListener
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.Command.Companion.replayAnalysis
import de.tectoast.emolga.commands.music.custom.WirklichGuteMusikCommand
import de.tectoast.emolga.modals.ModalListener
import de.tectoast.emolga.selectmenus.MenuListener
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.DASORID
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.DexQuiz
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.Shinycount
import de.tectoast.emolga.utils.sql.managers.BanManager
import de.tectoast.emolga.utils.sql.managers.MuteManager
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import java.net.URL

object EmolgaListener : ListenerAdapter() {

    fun registerEvents(jda: JDA) {
        jda.listener<ButtonInteractionEvent> { ButtonListener.check(it) }
        jda.listener<ModalInteractionEvent> { ModalListener.check(it) }
        jda.listener<SelectMenuInteractionEvent> { MenuListener.check(it) }
        jda.listener<MessageReceivedEvent> { messageReceived(it) }
        jda.listener<SlashCommandInteractionEvent> { slashCommandInteractionEvent(it) }
        jda.listener<ReadyEvent> { e ->
            if (e.jda.selfUser.idLong == 723829878755164202) {
                Emolga.get.drafts.values.filter { it.isRunning }.forEach {
                    it.startDraft(
                        null, true
                    )
                }
            }
        }
    }

    private suspend fun slashCommandInteractionEvent(e: SlashCommandInteractionEvent) {
        val command = Command.byName(e.name) ?: return
        val u = e.user
        val tco = e.channel.asTextChannel()
        val mem = e.member!!
        if (command.disabled) return
        val gid = e.guild!!.idLong
        if (!command.checkBot(e.jda, gid)) return
        val check = command.checkPermissions(gid, mem)
        if (check == Command.PermissionCheck.GUILD_NOT_ALLOWED) return
        if (check == Command.PermissionCheck.PERMISSION_DENIED) {
            e.reply_(Command.NOPERM, ephemeral = true).queue()
            return
        }
        if (mem.idLong != FLOID) {
            if (Command.BOT_DISABLED) {
                e.reply(Command.DISABLED_TEXT).queue()
                return
            }
            if (command.wip) {
                e.reply("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!").queue()
                return
            }
        }
        try {
            GuildCommandEvent(command, e).execute()
        } catch (ex: Command.MissingArgumentException) {
            val arg = ex.argument!!
            e.reply(
                arg.customErrorMessage
                    ?: """Das benötigte Argument `${arg.name}`, was eigentlich ${Command.buildEnumeration(arg.type.getName())} sein müsste, ist nicht vorhanden!
Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help ${command.name}`.""".trimIndent()
            ).queue()
            if (u.idLong != FLOID) {
                Command.sendToMe("MissingArgument " + tco.asMention + " Server: " + tco.guild.name)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            e.hook.sendMessage(
                """
    Es ist ein Fehler beim Ausführen des Commands aufgetreten!
    Wenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei ${Constants.MYTAG}.
    ${command.getHelp(e.guild)}${if (u.idLong == FLOID) "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" else ""}
    """.trimIndent()
            ).queue()
        }

    }

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        val g = e.guild
        val mem = e.member
        when (g.idLong) {
            Constants.G.ASL -> g.getTextChannelById(615605820381593610L)!!.sendMessage(
                """
                    Willkommen auf der ASL, ${mem.asMention}. <:hi:540969951608045578>
                    Dies ist ein Pokémon Server mit dem Fokus auf einem kompetetiven Draftligasystem. Mach dich mit dem <#635765395038666762> vertraut und beachte die vorgegebenen Themen der Kanäle. Viel Spaß! <:yay:540970044297838597>
                    """.trimIndent()
            ).queue()
        }
    }

    override fun onGuildVoiceMove(e: GuildVoiceMoveEvent) {
        if (e.channelLeft.members.size == 1 && e.guild.audioManager.isConnected) e.guild.audioManager.closeAudioConnection()
    }

    override fun onGuildVoiceJoin(e: GuildVoiceJoinEvent) {
        if (e.channelJoined.idLong == 979436321359683594L) {
            val mid = e.member.idLong
            if (mid == e.jda.selfUser.idLong) return
            WirklichGuteMusikCommand.doIt(
                e.guild.getTextChannelById(861558360104632330L)!!, e.member, mid == FLOID || mid == DASORID
            )
        }
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        val g = e.guild
        e.jda.openPrivateChannelById(g.ownerIdLong).flatMap {
            it.sendMessage(
                WELCOMEMESSAGE.replace(
                    "{USERNAME}", "<@${g.ownerIdLong}>"
                ).replace("{SERVERNAME}", g.name)
            )
        }.queue()
        e.jda.openPrivateChannelById(FLOID).flatMap {
            it.sendMessage("${g.name} (${g.id})").setActionRow(
                Button.primary("guildinvite;" + g.id, "Invite").withEmoji(
                    Emoji.fromUnicode("✉️")
                )
            )
        }.queue()
    }

    override fun onRoleCreate(e: RoleCreateEvent) {
        when (e.guild.idLong) {
            736555250118295622, 447357526997073930, 518008523653775366 -> e.role.manager.revokePermissions(Permission.CREATE_INSTANT_INVITE)
                .queue()
        }
    }

    private suspend fun messageReceived(e: MessageReceivedEvent) {
        if (e.channelType == ChannelType.TEXT) {
            try {
                if (e.isWebhookMessage) return
                val m = e.message
                val msg = m.contentDisplay
                val tco = e.channel.asTextChannel()
                val member = e.member!!
                val g = e.guild
                val gid = g.idLong
                val tcoid = tco.idLong
                Command.check(e)
                if (gid == Constants.G.MY && member.idLong == FLOID) {
                    PrivateCommands.execute(e.message)
                    if (tco.parentCategoryIdLong == EMOLGA_KI) {
                        val split = tco.name.split("-")
                        e.jda.getTextChannelById(split[split.size - 1])!!.sendMessage(m.contentRaw).queue()
                    }
                }
                when (tcoid) {
                    929841771276554260 -> g.addRoleToMember(member, g.getRoleById(934810601216147477L)!!).queue()
                    1006493531608723517, 714941684839874600 -> {
                        if (msg.startsWith("!entweder", ignoreCase = true)) {
                            val split = msg.substringAfter(" ").split(" oder ").toMutableList()
                            if (split.size > 1) {
                                tco.sendMessage(split.random().replace("?", "") + "!").queue()
                            }
                        }
                    }
                }

                val raw = m.contentRaw
                val id = e.jda.selfUser.idLong
                if (raw == "<@!$id>" || raw == "<@$id>" && !e.author.isBot && Command.isChannelAllowed(tco)) {
                    Command.help(tco, member)
                }
                if (tcoid in Command.emoteSteal) {
                    val l = m.mentions.customEmojis
                    for (emote in l) {
                        g.createEmoji(emote.name, Icon.from(withContext(Dispatchers.IO) {
                            URL(emote.imageUrl).openStream()
                        })).queue()
                    }
                }
                if (tcoid == 778380440078647296L || tcoid == 919641011632881695L) {
                    val split = msg.split(" ")
                    val counter = Shinycount.get.counter
                    counter.entries.firstOrNull { it.key.startsWith(split[1], ignoreCase = true) }?.value?.let { map ->
                        var isCmd = true
                        val mem = member.idLong.ifMatches(598199247124299776) { it == 893773494578470922 }.toString()
                        if (msg.startsWith("!set ", ignoreCase = true)) {
                            map[mem] = split[2].toLong()
                        } else if (msg.startsWith("!add ", ignoreCase = true)) {
                            map.compute(mem) { _, v -> (v ?: 0) + split[2].toLong() }
                        } else if (msg.startsWith("!reset", ignoreCase = true)) {
                            map[mem] = 0
                        } else {
                            isCmd = false
                        }
                        if (isCmd) {
                            m.delete().queue()
                            Command.updateShinyCounts(tcoid)
                        }
                    }
                    /*val counter = Command.shinycountjson.getJSONObject("counter")
                    counter.keySet().firstOrNull { it.lowercase().startsWith(split[1].lowercase()) }?.let {
                        val o = Command.shinycountjson.getJSONObject("counter").getJSONObject(it)
                        var isCmd = true
                        val mid = if (member.id == "893773494578470922") "598199247124299776" else member.id
                        if (msg.contains("!set ")) {
                            o.put(mid, split[2].toInt())
                        } else if (msg.contains("!reset ")) {
                            o.put(mid, 0)
                        } else if (msg.contains("!add ")) {
                            o.put(mid, o.optInt(mid, 0) + split[2].toInt())
                        } else isCmd = false
                        if (isCmd) {
                            m.delete().queue()
                            Command.updateShinyCounts(tcoid)
                        }
                    }*/
                }
                if (!e.author.isBot && !msg.startsWith("!dexquiz")) {
                    DexQuiz.getByTC(tco)?.run {
                        if (nonBlocking()) {
                            if (check(msg)) {
                                block()
                                tco.sendMessage("${member.asMention} hat das Pokemon erraten! Es war **${currentGerName}**! (Der Eintrag stammt aus **Pokemon ${currentEdition}**)")
                                    .queue()
                                givePoint(member.idLong)
                                nextRound()
                            }
                        }
                    }
                }
                if (replayAnalysis.containsKey(tcoid) && e.author.id != e.jda.selfUser.id && !msg.contains("!analyse ") && !msg.contains(
                        "!sets "
                    )
                ) {
                    val t = tco.guild.getTextChannelById(replayAnalysis.getValue(tcoid)) ?: return
                    //t.sendTyping().queue();
                    urlRegex.find(msg)?.run {
                        val url = groupValues[0]
                        logger.info(url)
                        Command.analyseReplay(url, null, t, m, null)
                    }
                }
            } catch (ex: IllegalStateException) {
                Command.sendToMe(e.channelType.name + " IllegalStateException Channel")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else if (e.isFromType(ChannelType.PRIVATE)) {
            if (e.author.isBot) return
            if (e.author.idLong != FLOID) e.jda.getTextChannelById(828044461379682314L)
                ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.queue()
            PrivateCommand.check(e)
            val msg = e.message.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                urlRegex.find(msg)?.run {
                    val url = groupValues[0]
                    logger.info(url)
                    Command.analyseReplay(
                        url,
                        e.jda.getTextChannelById(999779545316069396),
                        e.jda.getTextChannelById(999779578799202324)!!,
                        e.message,
                        null
                    )
                }
            }
        }
    }

    override fun onGuildVoiceLeave(e: GuildVoiceLeaveEvent) {
        if (e.member.idLong == e.jda.selfUser.idLong) {
            val manager = Command.getGuildAudioPlayer(e.guild)
            manager.scheduler.queue.clear()
            manager.scheduler.nextTrack()
        }
        if (e.channelLeft.members.size == 1 && e.guild.audioManager.isConnected) {
            e.guild.audioManager.closeAudioConnection()
        }
    }

    override fun onReady(e: ReadyEvent) {
        Command.uninitializedCommands.forEach { Command.sendToMe("No Argument Manager Template: $it") }
        val jda = e.jda
        if (jda.selfUser.idLong == 723829878755164202L) {
            BanManager.forAll { set ->
                jda.getGuildById(set.getLong("guildid"))?.run {
                    Command.banTimer(
                        this, set.getTimestamp("expires")?.time ?: -1, set.getLong("userid")
                    )
                }
            }
            MuteManager.forAll { set ->
                jda.getGuildById(set.getLong("guildid"))?.run {
                    Command.muteTimer(
                        this, set.getTimestamp("expires")?.time ?: -1, set.getLong("userid")
                    )
                }
            }
            //Draft(jda.getTextChannelById(837425828245667841)!!, "NDS", null, fromFile = true, isSwitchDraft = true);
        }
    }

    override fun onGuildInviteCreate(e: GuildInviteCreateEvent) {
        val g = e.guild
        when (g.idLong) {
            Constants.G.ASL -> {
                g.retrieveMember(e.invite.inviter!!).queue { mem: Member? ->
                    if (g.selfMember.canInteract(
                            mem!!
                        )
                    ) e.invite.delete().queue()
                }
            }
        }
    }

    override fun onCommandAutoCompleteInteraction(e: CommandAutoCompleteInteractionEvent) {
        if (e.commandType == net.dv8tion.jda.api.interactions.commands.Command.Type.SLASH) {
            val focusedOption = e.focusedOption
            val arg = Command.byName(e.name)!!.argumentTemplate.find(focusedOption.name)!!
            val type = arg.type
            if (type.hasAutoComplete()) {
                type.autoCompleteList(focusedOption.value, e).run {
                    e.replyChoiceStrings(this ?: emptyList()).queue()
                }
            }
        }
    }

    private val WELCOMEMESSAGE: String = """
            Hallo **{USERNAME}** und vielen Dank, dass du mich auf deinen Server **{SERVERNAME}** geholt hast!
            Vermutlich möchtest du für deinen Server hauptsächlich, dass die Ergebnisse von Showdown Replays in einen Channel geschickt werden.
            Um mich zu konfigurieren gibt es folgende Möglichkeiten:

            **1. Die Ergebnisse sollen in den gleichen Channel geschickt werden:**
            Einfach `/replaychannel add` in den jeweiligen Channel schreiben

            **2. Die Ergebnisse sollen in einen anderen Channel geschickt werden:**
            `/replaychannel add #Ergebnischannel` in den Channel schicken, wo später die Replays reingeschickt werden sollen (Der #Ergebnischannel ist logischerweise der Channel, wo später die Ergebnisse reingeschickt werden sollen)

            Falls die Ergebnisse in ||Spoilertags|| geschickt werden sollen, schick irgendwo auf dem Server den Command `!spoilertags` rein. Dies gilt dann serverweit.

            Ich habe übrigens noch viele weitere Funktionen! Wenn du mich pingst, zeige ich dir eine Übersicht aller Commands :)
            Falls du weitere Fragen oder Probleme hast, schreibe ${Constants.MYTAG} eine PN :)
        """.trimIndent()
    private val logger = LoggerFactory.getLogger(EmolgaListener::class.java)
    private val urlRegex = Regex(
        "https://replay.pokemonshowdown.com\\b([-a-zA-Z\\d()@:%_+.~#?&/=]*)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
    )

}