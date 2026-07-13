package de.tectoast.emolga.features.system

import de.tectoast.generic.K18n_NoResults
import de.tectoast.generic.K18n_TooManyResults
import de.tectoast.k18n.generated.K18nLanguage

fun List<String>?.convertListToAutoCompleteReply(language: K18nLanguage) = when (this?.size) {
    0, null -> listOf(K18n_NoResults.translateTo(language))
    in 1..25 -> this
    else -> listOf(K18n_TooManyResults.translateTo(language))
}