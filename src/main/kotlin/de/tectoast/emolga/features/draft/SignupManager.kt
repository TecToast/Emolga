package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.database.exposed.YTChannelsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.draft.during.generic.K18n_NoSignupInGuild
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.*
import de.tectoast.emolga.utils.mapToChannelIdPair
import de.tectoast.emolga.utils.translateToGuildLanguage
import de.tectoast.generic.K18n_AlreadySignedUp
import de.tectoast.generic.K18n_SignupVerb
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
        override val label = K18n_SignupVerb
        override val emoji = Emoji.fromUnicode("✅")

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val lsData = mdb.signups.get(iData.gid)
                ?: return iData.reply(K18n_Signup.SignUpClosedError, ephemeral = true)
            if (lsData.getDataByUser(iData.user) != null) {
                return iData.reply(K18n_AlreadySignedUp, ephemeral = true)
            }
            val modal = lsData.buildModal(old = null)
            if (modal == null) {
                signupUser(gid = iData.gid, uid = iData.user, data = emptyMap(), iData = iData)
                return
            }
            iData.replyModal(modal)
        }
    }

    object UnsignupCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("unsignup", K18n_Signup.UnsignupHelp)) {

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val ligaStartData = mdb.signups.get(iData.gid) ?: return iData.reply(
                K18n_NoSignupInGuild, ephemeral = true
            )
            iData.reply(
                if (ligaStartData.deleteUser(iData.user)) K18n_Signup.UnsignupSuccess else K18n_Signup.NotSignedUp,
                ephemeral = true
            )
        }
    }

    object SignupChangeCommand : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "signupchange",
            K18n_Signup.SignupChangeHelp,
        )
    ) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val ligaStartData = mdb.signups.get(iData.gid) ?: return iData.reply(
                K18n_NoSignupInGuild, ephemeral = true
            )
            val signUpData = ligaStartData.getDataByUser(iData.user) ?: return iData.reply(
                K18n_Signup.NotSignedUp,
                ephemeral = true
            )
            val modal = ligaStartData.buildModal(signUpData) ?: return iData.reply(
                K18n_Signup.SignupChangeNoData,
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
                val ligaStartData = mdb.signups.get(gid) ?: return@registerListener
                ligaStartData.handleModal(it)
            }
        }
    }

    suspend fun createSignup(
        gid: Long,
        config: LigaStartConfig
    ) {
        if (mdb.signups.get(gid) != null) return
        val tc = jda.getTextChannelById(config.announceChannel)!!

        val messageid =
            tc.sendMessage(config.signupMessage.condAppend(!config.hideUserCount) {
                K18n_Signup.SignupMessageData(
                    "0",
                    config.maxUsers.takeIf { it > 0 }?.toString() ?: "?"
                ).translateToGuildLanguage(gid)
            })
                .addComponents(Button.withoutIData(language = GuildLanguageDB.getLanguage(gid)).into())
                .await().idLong
        mdb.signups.insertOne(
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
            with(mdb.signups.get(gid)!!) {
                if (!isChange && full) return iData?.reply(K18n_Signup.SignupFull)
                val jda = iData?.jda ?: jda
                if (isChange) {
                    val oldData = getDataByUser(uid) ?: return iData?.reply(K18n_Signup.NotSignedUp)
                    oldData.data.putAll(data)
                    handleSignupChange(oldData)
                    signupScope.launch {
                        logoAttachment?.handleLogo(uid, iData)
                    }
                    iData?.reply(K18n_Signup.DataChangeSuccessful)
                    return null
                }
                iData?.reply(K18n_Signup.SignupSuccess)
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
                signupScope.launch {
                    logoAttachment?.handleLogo(uid, iData)
                    @Suppress("DeferredResultUnused")
                    data[SignUpInput.SDNAME_ID]?.let {
                        SDNamesDB.addIfAbsent(it, uid)
                    }
                    data[SignUpInput.YT_CHANNEL_ID]?.let {
                        YTChannelsDB.insertSingle(uid, it.mapToChannelIdPair())
                    }
                    giveParticipantRole {
                        (iData?.member?.invoke()) ?: jda.getGuildById(gid)?.retrieveMember(UserSnowflake.fromId(uid))!!
                            .await()
                    }
                }
            }
        }
        return null
    }

    context(lsData: LigaStartData)
    private suspend fun Message.Attachment.handleLogo(uid: Long, iData: InteractionData?) {
        lsData.insertLogo(uid, this@handleLogo)?.let { errorStr ->
            iData?.reply(K18n_Signup.LogoError(errorStr.translateTo(iData.language)), ephemeral = true)
        }
    }
}
