package de.tectoast.emolga.utils.wizard

import de.tectoast.emolga.utils.K18n_Wizard

class WizardExecutor<State : Any, Input : Any>(steps: List<WizardStep<State, State, Input>>) {
    val groupedByState = steps.groupBy { it.stateClass }

    suspend fun execute(state: State, input: Input): WizardStepResult<State>? {
        val stepsForState = groupedByState[state::class] ?: error("No steps found for state ${state::class}")
        val step =
            stepsForState.singleOrNull { it.inputClass.isInstance(input) } ?: return WizardStepResult(
                state,
                K18n_Wizard.WrongInput
            )
        return step.execute(state, input)
    }
}