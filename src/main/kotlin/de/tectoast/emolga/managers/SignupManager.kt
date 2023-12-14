package de.tectoast.emolga.managers

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.SignUpData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.util.concurrent.ConcurrentHashMap

object SignupManager {
    suspend fun createSignup(
        announceChannel: Long,
        signupChannel: Long,
        logoChannel: Long,
        maxUsers: Int,
        roleId: Long?,
        withExperiences: Boolean,
        text: String
    ) {
        val tc = jda.getTextChannelById(announceChannel)!!
        val messageid =
            tc.sendMessage(
                text + "\n\n**Teilnehmer: 0/${maxUsers.takeIf { it > 0 } ?: "?"}**")
                .addActionRow(
                    primary(
                        "signup", "Anmelden", Emoji.fromUnicode("✅")
                    )
                ).await().idLong
        db.signups.insertOne(
            LigaStartData(
                guild = tc.guild.idLong,
                signupChannel = signupChannel,
                logoChannel = logoChannel,
                maxUsers = maxUsers,
                announceChannel = tc.idLong,
                announceMessageId = messageid,
                participantRole = roleId,
                signupMessage = text,
                withExperiences = withExperiences
            )
        )
    }

    private val persistentSignupData = ConcurrentHashMap<Long, Pair<Mutex, Channel<LigaStartData>>>()
    private val signupScope = CoroutineScope(Dispatchers.IO)

    suspend fun signupUser(
        gid: Long,
        uid: Long,
        sdname: String,
        teamname: String?,
        experiences: String?,
        isChange: Boolean = false,
        e: ModalInteractionEvent? = null
    ): Unit? {
        val sdnameid = Command.toUsername(sdname)
        if (sdnameid.length !in 1..18) return e?.reply_("Dieser Showdown-Name ist ungültig!")?.setEphemeral(true)
            ?.queue()
        e?.deferReply(true)?.queue()
        val (signupMutex, channel) = persistentSignupData.getOrPut(gid) {
            val c = Channel<LigaStartData>(Channel.CONFLATED)
            signupScope.launch {
                while (true) {
                    c.receive().updateSignupMessage()
                    delay(10000)
                }
            }
            Mutex() to c
        }
        signupMutex.withLock {
            with(db.signups.get(gid)!!) {
                if (!isChange && full) return e?.hook?.sendMessage("❌ Die Anmeldung ist bereits voll!")?.queue()
                val ownerOfTeam = PrivateCommands.userIdForSignupChange?.takeIf { uid == Constants.FLOID }
                    ?: if (isChange) getOwnerByUser(uid) ?: return e?.reply_(
                        "Du bist derzeit nicht angemeldet!"
                    )?.setEphemeral(true)?.queue()
                    else uid
                if (ownerOfTeam in users && !isChange) return e?.reply_("Du bist bereits angemeldet!")
                    ?.setEphemeral(true)?.queue()

                @Suppress("DeferredResultUnused") SDNamesDB.addIfAbsent(sdname, ownerOfTeam)
                val jda = e?.jda ?: jda
                if (isChange) {
                    val data = users[ownerOfTeam]!!
                    data.sdname = sdname
                    data.teamname = teamname
                    data.experiences = experiences
                    e?.hook?.sendMessage("Deine Daten wurden erfolgreich geändert!")?.queue()
                    jda.getTextChannelById(signupChannel)!!.editMessageById(
                        data.signupmid!!, data.toMessage(ownerOfTeam, this)
                    ).queue()
                    data.logomid?.let {
                        jda.getTextChannelById(logoChannel)!!
                            .editMessageById(it, "**Logo von <@$ownerOfTeam> ($teamname):**").queue()
                    }
                    save()
                    return null
                }
                e?.hook?.sendMessage("✅ Du wurdest erfolgreich angemeldet!")?.setEphemeral(true)?.queue()
                giveParticipantRole(
                    (e?.member) ?: jda.getGuildById(gid)?.retrieveMember(UserSnowflake.fromId(uid))!!.await()
                )
                val signUpData = SignUpData(
                    teamname, sdname, experiences = experiences
                ).apply {
                    signupmid = jda.getTextChannelById(signupChannel)!!.sendMessage(
                        toMessage(ownerOfTeam, this@with)
                    ).await().idLong
                }
                users[ownerOfTeam] = signUpData
                channel.send(this)
                if (full) {
                    closeSignup()
                }
                save()
            }
        }
        return null
    }
}
