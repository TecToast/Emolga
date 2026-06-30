package de.tectoast.emolga.features.system.model

import de.tectoast.generic.K18n_NoPermission
import de.tectoast.k18n.generated.K18nMessage

sealed class AllowedResult {
    companion object {
        operator fun invoke(b: Boolean) = if (b) Allowed else NotAllowed
    }
}

data object Allowed : AllowedResult()
open class NotAllowed(val reason: K18nMessage) : AllowedResult() {
    companion object : NotAllowed(K18n_NoPermission)
}