package de.tectoast.emolga.utils.wizard

import kotlin.reflect.KClass

abstract class WizardStep<GeneralState : Any, State : GeneralState, Input : Any>(
    val stateClass: KClass<State>,
    val inputClass: KClass<Input>
) {
    abstract suspend fun execute(state: State, input: Input): WizardStepResult<GeneralState>?
}