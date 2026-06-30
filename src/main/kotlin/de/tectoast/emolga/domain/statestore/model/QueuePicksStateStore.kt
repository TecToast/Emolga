package de.tectoast.emolga.domain.statestore.model

import de.tectoast.emolga.domain.league.queue.model.QueuePicksUserData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.utils.Language
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("QueuePicks")
@OptIn(ExperimentalSerializationApi::class)
class QueuePicksState : StateStore {

    val leaguename: String
    val guild: Long
    val tlLanguage: Language
    val idx: Int
    var currentlyEnabled = false
    val currentState: MutableList<QueuedAction>
    val addedMeanwhile: MutableList<QueuedAction> = mutableListOf()

    constructor(leaguename: String, guild: Long, tlLanguage: Language, idx: Int, currentData: QueuePicksUserData) {
        this.leaguename = leaguename
        this.guild = guild
        this.tlLanguage = tlLanguage
        this.idx = idx
        this.currentState = currentData.queued.toMutableList()
        this.currentlyEnabled = currentData.enabled
    }
}