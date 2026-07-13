package de.tectoast.emolga.domain.league.draft.model.execution

import de.tectoast.emolga.domain.league.draft.model.core.DraftActionOrigin
import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.SkipReason
import de.tectoast.emolga.domain.league.draft.util.DisplayHelper
import de.tectoast.emolga.utils.sheetupdate.SheetUpdateContext
import de.tectoast.k18n.generated.K18nMessage

sealed class DraftActionResult {
    open var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    val deletesMessage: MutableSet<Long> = mutableSetOf()
    val sendsMessage: MutableList<suspend (DisplayHelper) -> K18nMessage> = mutableListOf()

    abstract val round: Int
    abstract val idx: Int


    data class UserAction(
        override val round: Int,
        override val idx: Int,
        val forRound: Int,
        val origin: DraftActionOrigin,
        val input: DraftInput,
        val byUser: Long?,
        val showTier: String?,
        override var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    ) : DraftActionResult()

    data class Skip(
        override val round: Int, override val idx: Int, val reason: SkipReason,
    ) : DraftActionResult()

    data class UserFinished(override val round: Int, override val idx: Int) : DraftActionResult()

    data class DraftFinished(val msg: K18nMessage) : DraftActionResult() {
        override val round = -1
        override val idx = -1

        init {
            sendsMessage += { msg }
        }
    }
}
