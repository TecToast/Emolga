package de.tectoast.emolga.domain.league.tierlist.service.setup.wizard

import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.utils.wizard.WizardStep
import kotlin.reflect.KClass

sealed class TierlistWizardStep<State : TierlistWizardState, Input : TierlistWizardInput>(
    stateClass: KClass<State>,
    inputClass: KClass<Input>
) : WizardStep<TierlistWizardState, State, Input>(stateClass, inputClass)