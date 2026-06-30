package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.StringSelectMenuFeature
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single
import org.koin.core.component.get


@Single(binds = [ListenerProvider::class])
class QueuePicksMenu(private val stateStore: StateStoreDispatcher) :
    StringSelectMenuFeature<QueuePicksMenu.Args>(::Args, SelectMenuSpec("queuepicks")) {
    class Args : Arguments() {
        var mon by singleOption()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        stateStore.process<_, QueuePicksStateStoreHandler>(iData.user) {
            with(get<QueuePicksComponents>()) {
                handleSelect(e.mon.toShowdownID())
            }
        }
    }
}