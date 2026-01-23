package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.*
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.util.concurrent.ConcurrentHashMap

object SignupManager {

    private val persistentSignupData = ConcurrentHashMap<Long, Pair<Mutex, Channel<LigaStartData>>>()
    private val signupScope = createCoroutineScope("Signup", Dispatchers.IO)

    object Button : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("signup")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Anmelden"
        override val emoji = Emoji.fromUnicode("✅")

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val lsData = db.signups.get(iData.gid)
                ?: return iData.reply("Diese Anmeldung ist bereits geschlossen!", ephemeral = true)
            if (lsData.getDataByUser(iData.user) != null) {
                return iData.reply("Du bist bereits angemeldet!", ephemeral = true)
            }
            val modal = lsData.buildModal(old = null)
            if (modal == null) {
                signupUser(gid = iData.gid, uid = iData.user, data = emptyMap(), iData = iData)
                return
            }
            iData.replyModal(modal)
        }
    }

    object UnsignupCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("unsignup", "Ziehe deine Anmeldung zurück")) {

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val ligaStartData = db.signups.get(iData.gid) ?: return iData.reply(
                "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
            )
            iData.reply(
                if (ligaStartData.deleteUser(iData.user)) "✅ Deine Anmeldung wurde erfolgreich zurückgezogen!" else "❌ Du bist derzeit nicht angemeldet!",
                ephemeral = true
            )
        }
    }

    object SignupChangeCommand : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "signupchange",
            "Ermöglicht es dir, deine Anmeldung anzupassen",
        )
    ) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val ligaStartData = db.signups.get(iData.gid) ?: return iData.reply(
                "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
            )
            val signUpData = ligaStartData.getDataByUser(iData.user) ?: return iData.reply(
                "Du bist derzeit nicht angemeldet!",
                ephemeral = true
            )
            val modal = ligaStartData.buildModal(signUpData) ?: return iData.reply(
                "Es gibt keine Daten, die du ändern kannst!",
                ephemeral = true
            )
            iData.replyModal(modal)
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

    suspend fun createSignup(
        gid: Long,
        config: LigaStartConfig
    ) {
        if (db.signups.get(gid) != null) return
        val tc = jda.getTextChannelById(config.announceChannel)!!
        val messageid =
            tc.sendMessage(config.signupMessage.condAppend(!config.hideUserCount) { "\n\n**Teilnehmer: 0/${config.maxUsers.takeIf { it > 0 } ?: "?"}**" })
                .addComponents(Button().into())
                .await().idLong
        db.signups.insertOne(
            LigaStartData(
                guild = tc.guild.idLong,
                config = config,
                announceMessageId = messageid,
            )
        )
    }


    suspend fun signupUser(
        gid: Long,
        uid: Long,
        data: Map<String, String>,
        logoAttachment: Message.Attachment? = null,
        isChange: Boolean = false,
        iData: InteractionData? = null
    ): Unit? {
        iData?.ephemeralDefault()
        iData?.deferReply(true)
        val (signupMutex, channel) = persistentSignupData.getOrPut(gid) {
            val c = Channel<LigaStartData>(Channel.CONFLATED)
            signupScope.launch {
                while (true) {
                    val receive = c.receive()
                    if (!receive.config.hideUserCount) {
                        receive.updateSignupMessage()
                    }
                    delay(10000)
                }
            }
            Mutex() to c
        }
        signupMutex.withLock {
            with(db.signups.get(gid)!!) {
                if (!isChange && full) return iData?.reply("❌ Die Anmeldung ist bereits voll!")
                @Suppress("DeferredResultUnused")
                data[SignUpInput.SDNAME_ID]?.let {
                    SDNamesDB.addIfAbsent(it, uid)
                }
                val jda = iData?.jda ?: jda
                if (isChange) {
                    val oldData = getDataByUser(uid) ?: return iData?.reply("Du bist derzeit nicht angemeldet!")
                    oldData.data.putAll(data)
                    handleSignupChange(oldData)
                    logoAttachment?.handleLogo(uid, iData)
                    iData?.reply("Deine Daten wurden erfolgreich geändert!")
                    return null
                }
                iData?.reply("✅ Du wurdest erfolgreich angemeldet!")
                giveParticipantRole {
                    (iData?.member?.invoke()) ?: jda.getGuildById(gid)?.retrieveMember(UserSnowflake.fromId(uid))!!
                        .await()
                }
                val signUpData = SignUpData(
                    users = mutableSetOf(uid), data = data.toMutableMap()
                ).apply {
                    signupmid = jda.getTextChannelById(config.signupChannel)!!.sendMessage(
                        toMessage(this@with)
                    ).await().idLong
                }
                users.size
                users += signUpData
                channel.send(this)
                if (full) {
                    closeSignup()
                }
                save()
                logoAttachment?.handleLogo(uid, iData)
            }
        }
        return null
    }

    context(lsData: LigaStartData)
    private fun Message.Attachment.handleLogo(uid: Long, iData: InteractionData?) {
        signupScope.launch {
            lsData.insertLogo(uid, this@handleLogo)?.let { errorStr ->
                iData?.reply("⚠️ Dein Logo konnte nicht hochgeladen werden: $errorStr")
            }
        }
    }
}
