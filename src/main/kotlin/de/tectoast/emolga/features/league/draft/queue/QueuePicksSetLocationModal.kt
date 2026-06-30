package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import org.koin.core.annotation.Single
import org.koin.core.component.get

@Single(binds = [ListenerProvider::class])
class QueuePicksSetLocationModal(private val stateStore: StateStoreDispatcher) :
    ModalFeature<QueuePicksSetLocationModal.Args>(::Args, ModalSpec("queuepickslocation")) {
    override val title = K18n_QueuePicks.SetLocationModalTitle

    class Args : Arguments() {
        var mon by showdownIDArg().compIdOnly()
        var location by string<Int>("Position") {
            validate { it.toIntOrNull() }
            modal(placeholder = K18n_QueuePicks.SetLocationModalArgLocation)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        stateStore.process<_, QueuePicksStateStoreHandler>(iData.user) {
            with(get<QueuePicksComponents>()) {
                setLocation(e.mon, e.location)
            }
        }
    }
}