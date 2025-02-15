package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.SignUpData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
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
            if (lsData.getDataByUser(user) != null) {
                return reply("Du bist bereits angemeldet!", ephemeral = true)
            }
            val modal = lsData.buildModal(old = null)
            if (modal == null) {
                signupUser(gid, user, emptyMap(), e = self)
                return
            }
            replyModal(modal)
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
            val signUpData = ligaStartData.getDataByUser(user) ?: return reply(
                "Du bist derzeit nicht angemeldet!",
                ephemeral = true
            )
            val modal = ligaStartData.buildModal(signUpData)
            if (modal == null) return reply("Es gibt keine Daten, die du ändern kannst!", ephemeral = true)
            replyModal(modal)
        }
    }

    object ModalHandler : ListenerProvider() {
        init {
            registerListener<ModalInteractionEvent> {
                if (!it.modalId.startsWith("signup;")) return@registerListener
                val gid = it.guild?.idLong ?: return@registerListener
                val ligaStartData = db.signups.get(gid) ?: return@registerListener
                ligaStartData.handleModal(it)
            }
        }
    }


    suspend fun signupUser(
        gid: Long,
        uid: Long,
        data: Map<String, String>,
        isChange: Boolean = false,
        e: InteractionData? = null
    ): Unit? {
        e?.ephemeralDefault()
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
                @Suppress("DeferredResultUnused")
                data["sdname"]?.let {
                    SDNamesDB.addIfAbsent(it, uid)
                }
                val jda = e?.jda ?: jda
                if (isChange) {
                    val oldData = getDataByUser(uid) ?: return e?.reply("Du bist derzeit nicht angemeldet!")
                    oldData.data.putAll(data)
                    handleSignupChange(oldData)
                    e?.reply("Deine Daten wurden erfolgreich geändert!")
                    return null
                }
                e?.reply("✅ Du wurdest erfolgreich angemeldet!")
                giveParticipantRole {
                    (e?.member?.invoke()) ?: jda.getGuildById(gid)?.retrieveMember(UserSnowflake.fromId(uid))!!.await()
                }
                val signUpData = SignUpData(
                    users = mutableSetOf(uid), data = data.toMutableMap()
                ).apply {
                    signupmid = jda.getTextChannelById(signupChannel)!!.sendMessage(
                        toMessage(this@with)
                    ).await().idLong
                }
                users += signUpData
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
