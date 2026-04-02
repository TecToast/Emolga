package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.GeneralDiscordService.Companion.ROUTINE_MAINTENANCE_KEY
import de.tectoast.emolga.database.exposed.StatisticsRepository
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.features.FeatureManager
import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.json.only
import de.tectoast.emolga.utils.marker
import de.tectoast.emolga.utils.translateToGuildLanguage
import de.tectoast.generic.K18n_RoutineMaintenance
import dev.minn.jda.ktx.events.listener
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.litote.kmongo.ne

interface GeneralDiscordService {
    suspend fun updatePresence()
    suspend fun enableMaintenance(reason: String)
    suspend fun disableMaintenance()

    companion object {
        const val ROUTINE_MAINTENANCE_KEY = "ROUTINE"
    }
}

@Single
class ProductionGeneralDiscordService(
    val emolgajda: JDA,
    @Named("flegmon") val flegmonjda: JDA?,
    val featureManager: FeatureManager,
    private val statisticsRepository: StatisticsRepository
) : GeneralDiscordService, StartupTask {

    private val logger = KotlinLogging.logger {}
    var maintenance: String? = null

    override suspend fun enableMaintenance(reason: String) {
        maintenance = reason
        updatePresence()
    }

    override suspend fun disableMaintenance() {
        maintenance = null
        updatePresence()
    }

    override suspend fun onStartup() {

    }

    /**
     * Starts the discord bots
     */
    fun launchBots() {
        Message.suppressContentIntentWarning()

        emolgajda.listener<ReadyEvent> {
            logger.info("important".marker, "Emolga is now online!")
            mdb.league.find(League::draftState ne DraftState.OFF).toFlow().collect {
                logger.info("important".marker, "Starting draft ${it.leaguename}...")
                League.executeOnFreshLock({ it }) {
                    startDraft(null, true, null)
                }
            }
        }
    }

    /**
     * Starts the listeners for the discord bots
     */
    suspend fun startListeners() {
        mdb.config.only().maintenance?.let {
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
                featureManager.handleEvent(it)
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
    override suspend fun updatePresence() {
        if (maintenance != null) {
            emolgajda.presence.setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Maintenance"))
            return
        }
        val amountInLastSystem = 47474
        val count = statisticsRepository.getCurrentAmountOfReplays() + amountInLastSystem
        emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("$count analyzed replays"))
    }


}
