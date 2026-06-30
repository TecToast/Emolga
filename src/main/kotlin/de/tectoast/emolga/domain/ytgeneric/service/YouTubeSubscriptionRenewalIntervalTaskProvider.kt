package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTask
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey
import de.tectoast.emolga.domain.scheduling.interval.service.provider.IntervalTaskProvider
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.days

@Single
class YouTubeSubscriptionRenewalIntervalTaskProvider(private val ytSubscriptionStarter: YouTubeSubscriptionStarter) :
    IntervalTaskProvider {
    override val key = IntervalTaskKey("YTSubscriptionsRenewal")

    override fun provideTask() = IntervalTask(
        delay = 4.days,
        consumer = { ytSubscriptionStarter.setupYTSubscriptions() },
    )
}