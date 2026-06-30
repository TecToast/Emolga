package de.tectoast.emolga.domain.league.tierlist.service.setup

import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistSetupRepository
import de.tectoast.emolga.domain.league.tierlist.service.setup.wizard.TierlistWizardStep
import de.tectoast.emolga.utils.wizard.WizardExecutor
import de.tectoast.emolga.utils.wizard.WizardStepResult
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class TierlistSetupService(
    private val repo: TierlistSetupRepository,
    steps: List<TierlistWizardStep<TierlistWizardState, TierlistWizardInput>>
) {
    private val wizardExecutor = WizardExecutor(steps)

    suspend fun handleInput(code: Uuid, input: TierlistWizardInput): WizardStepResult<TierlistWizardState>? {
        val currentState = repo.getState(code) ?: return null
        val newState = wizardExecutor.execute(currentState, input)
        if (newState == null) {
            repo.deleteState(code)
            return null
        }
        repo.updateState(code, newState.state)
        return newState
    }
}