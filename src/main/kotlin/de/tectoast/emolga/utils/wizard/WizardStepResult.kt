package de.tectoast.emolga.utils.wizard

import de.tectoast.k18n.generated.K18nMessage

data class WizardStepResult<State : Any>(val state: State, val message: K18nMessage? = null)