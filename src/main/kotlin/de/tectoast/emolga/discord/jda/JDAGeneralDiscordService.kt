package de.tectoast.emolga.discord.jda

import de.tectoast.emolga.di.DiscordReadyTask
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.discord.OptionalJDA
import de.tectoast.emolga.discord.jdaOrNull
import de.tectoast.emolga.domain.config.repository.GlobalConfigRepository
import de.tectoast.emolga.domain.discord.service.GeneralDiscordService
import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.domain.statistics.repository.StatisticsRepository
import de.tectoast.emolga.features.system.FeatureEventHandler
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.generic.K18n_RoutineMaintenance
import dev.minn.jda.ktx.events.listener
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class JDAGeneralDiscordService(
    private val emolgajda: JDA,
    @Named("flegmon") private val flegmonjda: OptionalJDA,
    private val statisticsRepository: StatisticsRepository,
    private val globalConfigRepository: GlobalConfigRepository,
    private val languageRepo: GuildLanguageRepository,
    private val botConstants: BotConstants,
    private val discordReadyTasks: List<DiscordReadyTask>
) : GeneralDiscordService, StartupTask, KoinComponent {

    private val featureEventHandler: FeatureEventHandler by inject()

    override val priority = 1000

    private val logger = KotlinLogging.logger {}
    private var maintenance: String? = null
    private var lastReplayCount: Long? = null

    override suspend fun enableMaintenance(reason: String) {
        maintenance = reason
        globalConfigRepository.setMaintenanceMode(reason)
        updatePresence()
    }

    override suspend fun disableMaintenance() {
        maintenance = null
        globalConfigRepository.setMaintenanceMode(null)
        updatePresence()
    }

    override suspend fun onStartup() {
        maintenance = globalConfigRepository.getMaintenanceMode()
        startListeners()
    }

    /**
     * Starts the listeners for the discord bots
     */
    private suspend fun startListeners() {
        for (jda in listOfNotNull(emolgajda, flegmonjda.jdaOrNull)) {
            jda.listener<GenericEvent> {
                if (it is IReplyCallback && it.user.idLong != botConstants.botOwnerId) {
                    maintenance?.let { reason ->
                        it.reply(
                            if (reason == GeneralDiscordService.ROUTINE_MAINTENANCE_KEY) K18n_RoutineMaintenance.translateTo(
                                languageRepo.getLanguage(it.guild?.idLong)
                            ) else reason
                        ).setEphemeral(true).queue()
                        return@listener
                    }
                }
                featureEventHandler.handleEvent(it)
            }
            jda.awaitReady()
            if (jda == emolgajda)
                discordReadyTasks.forEach { it.onDiscordReady() }
        }
        updatePresence()
        logger.info("Discord Bots loaded!")
        flegmonjda.jdaOrNull?.presence?.activity = Activity.playing("mit seiner Rute")
    }

    /**
     * Updates the presence of Emolga to the current number of replays (or maintencance mode message)
     */
    override suspend fun updatePresence() {
        if (maintenance != null) {
            emolgajda.presence.setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Maintenance"))
            return
        }
        val amountInLastSystem = 47474
        val count = statisticsRepository.getCurrentAmountOfReplays() + amountInLastSystem
        if (lastReplayCount == count) return
        lastReplayCount = count
        emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("$count analyzed replays"))
    }


}
