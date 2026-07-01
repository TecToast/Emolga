package de.tectoast.emolga.features.flo.controlcentral

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.logging.LogConfigReloadService
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ControlCentralButton(private val logConfigReloadService: LogConfigReloadService, private val leagueConfigRepo: LeagueConfigRepository) :
    ButtonFeature<ControlCentralButton.Args>(::Args, ButtonSpec("controlcentral")) {

    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var mode by enumBasic<Mode>()
    }

    enum class Mode {
        BREAKPOINT,
        RELOAD_LOG_CONFIG,
        CLEAR_CONFIG_CACHE,
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        var breakpoint = false
        iData.deferReply(true)
        when (e.mode) {
            Mode.BREAKPOINT -> breakpoint = true
            Mode.RELOAD_LOG_CONFIG -> logConfigReloadService.reloadConfiguration()
            Mode.CLEAR_CONFIG_CACHE -> leagueConfigRepo.clearCache()
        }
        iData.replyRaw("Done!")
        if (breakpoint) {
            logger.info("") // I have a JVM breakpoint here (as it turns out, a simple Unit gets optimized away)
        }
    }
}