package de.tectoast.emolga.domain.statestore.model

import de.tectoast.emolga.features.league.draft.queue.*
import org.koin.core.annotation.Single

@Single
class QueuePicksComponents(
    val btn: QueuePicksControlButton,
    val menu: QueuePicksMenu,
    val finishBtn: QueuePicksFinishButton,
    val reloadBtn: QueuePicksReloadButton,
    val locationModal: QueuePicksSetLocationModal
)