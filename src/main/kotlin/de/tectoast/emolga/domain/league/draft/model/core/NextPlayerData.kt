package de.tectoast.emolga.domain.league.draft.model.core

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


sealed interface NextPlayerData {
    data object Normal : NextPlayerData
    data object InBetween : NextPlayerData
    data class Moved(val reason: SkipReason, val skippedUser: Int, val skippedBy: Long? = null) : NextPlayerData

}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isNormalPick(): Boolean {
    contract {
        returns(true) implies (this@isNormalPick is NextPlayerData.Normal)
    }
    return this is NextPlayerData.Normal
}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isMoved(): Boolean {
    contract {
        returns(true) implies (this@isMoved is NextPlayerData.Moved)
    }
    return this is NextPlayerData.Moved
}
