package de.tectoast.emolga.bot

import de.tectoast.emolga.buttons.ButtonListener
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.commands.marker
import de.tectoast.emolga.commands.showdown.ReplayCommand
import de.tectoast.emolga.database.exposed.BanDB
import de.tectoast.emolga.database.exposed.MuteDB
import de.tectoast.emolga.modals.ModalListener
import de.tectoast.emolga.selectmenus.MenuListener
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.reply_
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.litote.kmongo.eq

object EmolgaListener : ListenerAdapter() {

    fun registerEvents(jda: JDA) {
        jda.listener<ButtonInteractionEvent> { ButtonListener.check(it) }
        jda.listener<ModalInteractionEvent> { ModalListener.check(it) }
        jda.listener<StringSelectInteractionEvent> { MenuListener.check(it) }
        jda.listener<MessageReceivedEvent> { messageReceived(it) }
        jda.listener<SlashCommandInteractionEvent> { slashCommandInteractionEvent(it) }
        logger.info("important".marker, "Registering ready event...")
        jda.listener<ReadyEvent> { e ->
            logger.info("important".marker, "Ready event received!")
            if (e.jda.selfUser.idLong == 723829878755164202) {
                logger.info("important".marker, "Emolga is now online!")
                db.drafts.find(League::isRunning eq true).toList().forEach {
                    if (it.noAutoStart) return@forEach
                    logger.info("important".marker, "Starting draft ${it.leaguename}...")
                    it.startDraft(null, true, null)
                }
            }
        }
        jda.listener<CommandAutoCompleteInteractionEvent> { e ->
            if (e.commandType == net.dv8tion.jda.api.interactions.commands.Command.Type.SLASH) {
                val focusedOption = e.focusedOption
                val arg = Command.byName(e.name)!!.argumentTemplate.findForAutoComplete(focusedOption.name)!!
                val type = arg.type
                if (type.hasAutoComplete()) {
                    type.autoCompleteList(focusedOption.value, e).run {
                        e.replyChoiceStrings(this ?: emptyList()).queue()
                    }
                }
            }
        }
    }

    private suspend fun slashCommandInteractionEvent(e: SlashCommandInteractionEvent) {
        val command = Command.byName(e.name) ?: return
        val u = e.user
        val tco = e.channel.asGuildMessageChannel()
        val mem = e.member!!
        if (command.disabled) return
        val gid = e.guild!!.idLong
        if (!command.checkBot(e.jda)) return
        val check = command.checkPermissions(gid, mem)
        if (check == Command.PermissionCheck.GUILD_NOT_ALLOWED) return
        if (check == Command.PermissionCheck.PERMISSION_DENIED) {
            e.reply_(Command.NOPERM, ephemeral = true).queue()
            return
        }
        if (mem.idLong != FLOID && mem.idLong != 728202578353193010) {
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

    override fun onGuildVoiceUpdate(e: GuildVoiceUpdateEvent) {
        e.channelLeft?.let {
            if (it.members.size == 1 && e.guild.audioManager.isConnected) {
                e.guild.audioManager.closeAudioConnection()
            }
            if (e.channelJoined == null && e.member.idLong == e.jda.selfUser.idLong) {
                val manager = Command.getGuildAudioPlayer(e.guild)
                manager.scheduler.queue.clear()
                manager.scheduler.nextTrack()
            }
        }
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        val g = e.guild
        e.jda.openPrivateChannelById(g.ownerIdLong).flatMap {
            it.sendMessage(WELCOMEMESSAGE.replace("{USERNAME}", "<@${g.ownerIdLong}>").replace("{SERVERNAME}", g.name))
        }.queue()
        e.jda.openPrivateChannelById(FLOID).flatMap {
            it.sendMessage("${g.name} (${g.id})")
                .setActionRow(Button.primary("guildinvite;" + g.id, "Invite").withEmoji(Emoji.fromUnicode("✉️")))
        }.queue()
    }

    override fun onRoleCreate(e: RoleCreateEvent) {
        when (e.guild.idLong) {
            736555250118295622, 447357526997073930, 518008523653775366 -> e.role.manager.revokePermissions(Permission.CREATE_INSTANT_INVITE)
                .queue()
        }
    }

    private suspend fun messageReceived(e: MessageReceivedEvent) {
        when (e.channelType) {
            ChannelType.TEXT -> {
                if (e.isWebhookMessage) return
                if (e.jda.selfUser.idLong == 849569577343385601) Command.check(e)
            }

            ChannelType.PRIVATE -> {
                if (e.author.isBot) return
                if (e.author.idLong != FLOID) e.jda.getTextChannelById(828044461379682314L)
                    ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.apply {
                        if (e.message.attachments.isNotEmpty()) addContent("\n\n" + e.message.attachments.joinToString("\n") { it.url })
                    }?.queue()
                PrivateCommand.check(e)
                val msg = e.message.contentDisplay
                if (msg.contains("https://") || msg.contains("http://")) {
                    ReplayCommand.regex.find(msg)?.run {
                        val url = groupValues[0]
                        logger.info(url)
                        Command.analyseReplay(
                            url = url,
                            //customReplayChannel = e.jda.getTextChannelById(999779545316069396),
                            resultchannelParam = e.jda.getTextChannelById(820359155612254258)!!, message = e.message
                        )
                    }
                }
            }

            else -> {}
        }
    }

    override fun onReady(e: ReadyEvent) {
        val jda = e.jda
        if (jda.selfUser.idLong == 723829878755164202L) {
            Command.uninitializedCommands.forEach { Command.sendToMe("No Argument Manager Template: $it") }
            BanDB.schedule(jda)
            MuteDB.schedule(jda)
            //Draft(jda.getTextChannelById(837425828245667841)!!, "NDS", null, fromFile = true, isSwitchDraft = true);
        }
    }

    override fun onGuildInviteCreate(e: GuildInviteCreateEvent) {
        val g = e.guild
        when (g.idLong) {
            Constants.G.ASL -> {
                g.retrieveMember(e.invite.inviter!!).queue { mem: Member ->
                    if (g.selfMember.canInteract(mem)) e.invite.delete().queue()
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
            
            Wenn die Channel eingerichtet worden sind, muss man einfach /replay mit dem Replay-Link in einen Replay-Channel schicken und ich erledige den Rest.

            Falls die Ergebnisse in ||Spoilertags|| geschickt werden sollen, schick irgendwo auf dem Server den Command `/spoilertags` rein. Dies gilt dann serverweit.

            Falls du weitere Fragen oder Probleme hast, schreibe ${Constants.MYTAG} eine PN oder komme auf den Support-Server, dessen Link in meinem Profil steht :)
        """.trimIndent()
    private val logger = KotlinLogging.logger {}

}
