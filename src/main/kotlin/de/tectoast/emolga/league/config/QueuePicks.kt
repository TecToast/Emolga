package de.tectoast.emolga.league.config

import de.tectoast.emolga.utils.QueuedAction
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueuePicksConfig(
    val enabled: Boolean = true
)

@Serializable
data class QueuePicksData(
    val queuedPicks: MutableMap<Int, QueuePicksUserData> = mutableMapOf()
)

@Serializable
sealed interface PickNotification {
    fun wantsNotification(turn: Int): Boolean

    @Serializable
    @SerialName("Always")
    data object Always : PickNotification {
        override fun wantsNotification(turn: Int) = true
    }

    @Serializable
    @SerialName("Specified")
    data class Specified(val turns: Set<Int> = setOf()) : PickNotification {
        override fun wantsNotification(turn: Int) = turns.contains(turn)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class QueuePicksUserData(
    @EncodeDefault var enabled: Boolean = false,
    var disableIfSniped: Boolean = true,
    @EncodeDefault var queued: MutableList<QueuedAction> = mutableListOf(),
    val pickNotifications: PickNotification? = null
)
