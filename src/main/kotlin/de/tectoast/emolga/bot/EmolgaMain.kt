package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.credentials.Credentials
import de.tectoast.emolga.database.exposed.AnalysisStatistics
import de.tectoast.emolga.database.exposed.CmdManager
import de.tectoast.emolga.features.FeatureManager
import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import de.tectoast.generic.K18n_RoutineMaintenance
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
import org.litote.kmongo.ne
import org.slf4j.LoggerFactory

var injectedJDA: JDA? = null
    get() {
        if (field == null) {
            System.getenv("EMOLGATOKEN")?.let {
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
    var flegmonjda: JDA? = null
    var raikoujda: JDA? = null
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    val featureManager = OneTimeCache { FeatureManager("de.tectoast.emolga.features") }
    const val ROUTINE_MAINTENANCE_KEY = "ROUTINE"
    var maintenance: String? = null

    /**
     * Starts the discord bots
     */
    fun launchBots() {
        Message.suppressContentIntentWarning()
        emolgajda = default(Credentials.tokens.discord) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        emolgajda.listener<ReadyEvent> {
            logger.info("important".marker, "Emolga is now online!")
            db.league.find(League::draftState ne DraftState.OFF).toFlow().collect {
                logger.info("important".marker, "Starting draft ${it.leaguename}...")
                League.executeOnFreshLock({ it }) {
                    startDraft(null, true, null)
                }
            }
        }
        Credentials.tokens.discordflegmon?.let { flegmon ->
            flegmonjda = default(flegmon) {
                intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                setMemberCachePolicy(MemberCachePolicy.ALL)
            }
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

    /**
     * Starts the listeners for the discord bots
     */
    suspend fun startListeners() {
        launch {
            featureManager.updateCachedValue()
            CmdManager.startupCheck()
        }
        db.config.only().maintenance?.let {
            maintenance = it
        }
        for (jda in listOfNotNull(emolgajda, flegmonjda)) {
            jda.listener<GenericEvent> {
                if (it is IReplyCallback && it.user.idLong != Constants.FLOID) {
                    maintenance?.let { reason ->
                        it.reply(
                            if (reason == ROUTINE_MAINTENANCE_KEY) K18n_RoutineMaintenance.translateToGuildLanguage(
                                it.guild?.idLong
                            ) else reason
                        ).setEphemeral(true).queue()
                        return@listener
                    }
                }
                featureManager().handleEvent(it)
            }
            DConfiguratorManager.registerEvent(jda)
            jda.awaitReady()
        }
        updatePresence()
        logger.info("Discord Bots loaded!")
        flegmonjda?.presence?.activity = Activity.playing("mit seiner Rute")
    }

    /**
     * Updates the presence of Emolga to the current amount of replays (or maintencance mode message)
     */
    suspend fun updatePresence() {
        if (maintenance != null) {
            emolgajda.presence.setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Maintenance"))
            return
        }
        val amountInLastSystem = 47474
        val count = AnalysisStatistics.getCurrentAmountOfReplays() + amountInLastSystem
        emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("$count analyzed replays"))
    }


}
