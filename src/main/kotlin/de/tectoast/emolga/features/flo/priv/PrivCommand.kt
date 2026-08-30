package de.tectoast.emolga.features.flo.priv

import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey
import de.tectoast.emolga.domain.scheduling.interval.service.IntervalTaskService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class PrivCommand(restartYTSub: RestartYTSub) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("priv", "priv".k18n)) {

    override val children = listOf(restartYTSub)

    @Single
    class RestartYTSub(private val intervalTaskService: IntervalTaskService) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("restartytsub", "lol".k18n)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            intervalTaskService.restartTask(IntervalTaskKey("YTSubscriptionsRenewal"))
            iData.done()
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}