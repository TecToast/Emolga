package de.tectoast.emolga.league.config

import de.tectoast.emolga.utils.QueuedAction
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class QueuePicksConfig(
    val enabled: Boolean = true
)

@Serializable
data class QueuePicksData(
    val queuedPicks: MutableMap<Int, QueuePicksUserData> = mutableMapOf()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class QueuePicksUserData(
    @EncodeDefault var enabled: Boolean = false,
    var disableIfSniped: Boolean = true,
    @EncodeDefault var queued: MutableList<QueuedAction> = mutableListOf()
)