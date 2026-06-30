package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single
import org.koin.core.component.get


@Single(binds = [ListenerProvider::class])
class QueuePicksControlButton(private val stateStore: StateStoreDispatcher) :
    ButtonFeature<QueuePicksControlButton.Args>(::Args, ButtonSpec("queuepickscontrol")) {
    enum class ControlMode {
        UP, DOWN, REMOVE, CANCEL, MODAL
    }

    class Args : Arguments() {
        var mon by showdownIDArg()
        var controlMode by enumBasic<ControlMode>()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        stateStore.process<_, QueuePicksStateStoreHandler>(iData.user) {
            with(get<QueuePicksComponents>()) {
                handleButton(e)
            }
        }
    }
}