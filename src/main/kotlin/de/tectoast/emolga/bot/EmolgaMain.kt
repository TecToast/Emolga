package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.features.FeatureManager
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.getCount
import de.tectoast.emolga.utils.json.only
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.cache
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

var injectedJDA: JDA? = null
    get() {
        if (field == null) {
            System.getenv("DISCORDTOKEN")?.let {
                usedJDA = true
                field = default(it, intents = listOf()) {
                    cache -= listOf(VOICE_STATE, EMOJI, STICKER, SCHEDULED_EVENTS)
                }.awaitReady()
            }
        }
        return field
    }
var usedJDA = false
    private set
val jda: JDA by lazy { injectedJDA ?: emolgajda }

object EmolgaMain : CoroutineScope by createCoroutineScope("EmolgaMain") {
    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    var raikoujda: JDA? = null
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    val featureManager = OneTimeCache { FeatureManager("de.tectoast.emolga.features") }
    var maintenance: String? = null

    fun launchBots() {
        Message.suppressContentIntentWarning()
        emolgajda = default(Credentials.tokens.discord) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        emolgajda.listener<ReadyEvent> {
            logger.info("important".marker, "Emolga is now online!")
            db.drafts.find(League::isRunning eq true).toFlow().collect {
                if (it.noAutoStart) return@collect
                logger.info("important".marker, "Starting draft ${it.leaguename}...")
                it.startDraft(null, true, null)
            }
        }
        flegmonjda = default(Credentials.tokens.discordflegmon) {
            intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            setMemberCachePolicy(MemberCachePolicy.ALL)
        }
        defaultScope.launch {
            if (db.config.only().raikou) {
                Credentials.tokens.discordraikou.takeIf { it != "" }?.let {
                    raikoujda = default(it) {
                        intents += GatewayIntent.MESSAGE_CONTENT
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    suspend fun startListeners() {
        launch {
            featureManager.updateCachedValue()
        }
        db.config.only().maintenance?.let {
            maintenance = it
            updatePresence()
        }
        for (jda in listOf(emolgajda, flegmonjda)) {
            jda.listener<GenericEvent> {
                if (it is IReplyCallback && it.user.idLong != Constants.FLOID) {
                    maintenance?.let { reason ->
                        it.reply(reason).setEphemeral(true).queue()
                        return@listener
                    }
                }
                featureManager().handleEvent(it)
            }
            DConfiguratorManager.registerEvent(jda)
            jda.awaitReady()
        }
        logger.info("Discord Bots loaded!")
        ControlButtonSetup.init()
        flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
    }

    suspend fun updatePresence() {
        if (maintenance != null) {
            emolgajda.presence.setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Wartungsarbeiten"))
            return
        }
        val count = db.statistics.getCount("analysis")
        if (count % 100 == 0) {
            emolgajda.getTextChannelById(904481960527794217L)!!
                .sendMessage(SimpleDateFormat("dd.MM.yyyy").format(Date()) + ": " + count).queue()
        }
        emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("auf $count analysierte Replays"))
    }


}
