package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.SignUpData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toUsername
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.util.concurrent.ConcurrentHashMap

object SignupManager {

    private val persistentSignupData = ConcurrentHashMap<Long, Pair<Mutex, Channel<LigaStartData>>>()
    private val signupScope = createCoroutineScope("Signup", Dispatchers.IO)

    object Button : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("signup")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Anmelden"
        override val emoji = Emoji.fromUnicode("✅")

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val lsData = db.signups.get(gid)
                ?: return reply("Diese Anmeldung ist bereits geschlossen!", ephemeral = true)
            if (user in lsData.users || lsData.users.values.any { user in it.teammates }) {
                return reply("Du bist bereits angemeldet!", ephemeral = true)
            }
            replyModal(getModal(null, lsData))
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("signup")) {
        class Args : Arguments() {
            var change by boolean().compIdOnly()
            var teamname by string("Team-Name") {
                modal {
                    setRequiredRange(1, 100)
                }
            }.defaultNotEnabled(TEAMNAME, required = true)
            var sdname by string("Showdown-Name") {
                modal {
                    setRequiredRange(1, 18)
                }
            }
            var experiences by string("Erfahrungen") {
                modal(short = false) {
                    placeholder = "Wie viel Erfahrung hast du im CP-Bereich?"
                    setRequiredRange(1, 100)
                }
            }.defaultNotEnabled(EXPERIENCES)
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            signupUser(gid, user, e.sdname, e.teamname, e.experiences, e.change, self)
        }
    }

    object SignupChangeCommand : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "signupchange",
            "Ermöglicht es dir, deine Anmeldung anzupassen",
            *draftGuilds
        )
    ) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val ligaStartData = db.signups.get(gid) ?: return reply(
                "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
            )
            val signUpData =
                ligaStartData.users[PrivateCommands.userIdForSignupChange?.takeIf { user == Constants.FLOID }]
                    ?: ligaStartData.getDataByUser(user) ?: return reply(
                        "Du bist derzeit nicht angemeldet!",
                        ephemeral = true
                    )
            replyModal(getModal(signUpData, ligaStartData))
        }
    }

    object TEAMNAME : ModalKey
    object EXPERIENCES : ModalKey

    fun getModal(data: SignUpData?, lsData: LigaStartData) =
        Modal(
            "Anmeldung".condAppend(data != null, "sanpassung"),
            specificallyEnabledArgs = mapOf(
                TEAMNAME to !lsData.noTeam,
                EXPERIENCES to lsData.withExperiences
            )
        ) {
            change = data != null
            data?.let {
                teamname = it.teamname
                sdname = it.sdname
                experiences = it.experiences
            }

        }

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
                .addActionRow(Button()).await().idLong
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


    suspend fun signupUser(
        gid: Long,
        uid: Long,
        sdname: String?,
        teamname: String?,
        experiences: String?,
        isChange: Boolean = false,
        e: InteractionData? = null
    ): Unit? {
        e?.ephemeralDefault()
        sdname?.let {
            if (it.toUsername().length !in 1..18) return e?.reply("Dieser Showdown-Name ist ungültig!")
        }
        e?.deferReply(true)
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
                if (!isChange && full) return e?.reply("❌ Die Anmeldung ist bereits voll!")
                val ownerOfTeam = PrivateCommands.userIdForSignupChange?.takeIf { uid == Constants.FLOID }
                    ?: if (isChange) getOwnerByUser(uid) ?: return e?.reply(
                        "Du bist derzeit nicht angemeldet!", ephemeral = true
                    )
                    else uid
                if (ownerOfTeam in users && !isChange) return e?.reply("Du bist bereits angemeldet!")
                @Suppress("DeferredResultUnused")
                sdname?.let {
                    SDNamesDB.addIfAbsent(it, ownerOfTeam)
                }
                val jda = e?.jda ?: jda
                if (isChange) {
                    val data = users[ownerOfTeam]!!
                    data.sdname = sdname ?: "EMPTY"
                    data.teamname = teamname
                    data.experiences = experiences
                    e?.reply("Deine Daten wurden erfolgreich geändert!")
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
                e?.reply("✅ Du wurdest erfolgreich angemeldet!")
                giveParticipantRole {
                    (e?.member()) ?: jda.getGuildById(gid)?.retrieveMember(UserSnowflake.fromId(uid))!!.await()
                }
                val signUpData = SignUpData(
                    teamname, sdname ?: "EMPTY", experiences = experiences
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
