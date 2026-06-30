package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single
import org.koin.core.component.get


@Single(binds = [ListenerProvider::class])
class QueuePicksReloadButton(private val stateStore: StateStoreDispatcher) :
    ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("queuepicksreload")) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = K18n_QueuePicks.ReloadLabel
    override val emoji = Emoji.fromUnicode("\uD83D\uDD04")

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        stateStore.process<_, QueuePicksStateStoreHandler>(iData.user) {
            with(get<QueuePicksComponents>()) {
                reload()
            }
        }
    }
}