package de.tectoast.emolga.domain.league.queue.model

import kotlinx.serialization.Serializable

@Serializable
data class QueuePicksUserData(
    var enabled: Boolean = false,
    var disableIfSniped: Boolean = true,
    var notifyOnSuccess: Boolean = false,
    var queued: MutableList<QueuedAction> = mutableListOf(),
)
