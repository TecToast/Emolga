package de.tectoast.emolga.domain.statestore.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class StateStore {

    @Transient
    var forDeletion = false

    fun delete() {
        forDeletion = true
    }
}