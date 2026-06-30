package de.tectoast.emolga.domain.league.draft.service.timer.calc

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single
import kotlin.time.Instant


@Single
class DraftTimerDispatcher(handlers: List<DraftTimerHandler<DraftTimerConfig>>) :
    DraftTimerOperations<DraftTimerConfig> {
    private val registry = HandlerRegistry(handlers)
    override fun getCurrentTimerInfo(
        config: DraftTimerConfig,
        now: Instant
    ) = registry.getHandler(config).getCurrentTimerInfo(config, now)

    override fun shouldCancelOnZeroDelay(config: DraftTimerConfig) =
        registry.getHandler(config).shouldCancelOnZeroDelay(config)
}