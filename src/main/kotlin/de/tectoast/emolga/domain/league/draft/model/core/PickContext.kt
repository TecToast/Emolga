package de.tectoast.emolga.domain.league.draft.model.core

sealed interface PickContext {

    val idx: Int

    data class RegularTurn(override val idx: Int) : PickContext

    data class AfterDraftUnordered(override val idx: Int) : PickContext

    data class InBetweenPick(override val idx: Int, val isActualCurrent: Boolean) : PickContext

}
