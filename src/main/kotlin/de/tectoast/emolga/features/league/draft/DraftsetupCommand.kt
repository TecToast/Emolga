package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DraftsetupCommand(private val draftService: DraftService) : CommandFeature<DraftsetupCommand.Args>(
    ::Args,
    CommandSpec("draftsetup", K18n_Draftsetup.Help)
) {
    class Args : Arguments() {
        var name by string("Name", K18n_Draftsetup.ArgName)
        var switchdraft by boolean("switchdraft", K18n_Draftsetup.ArgSwitchDraft) {
            default = false
        }
    }

    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw("+1", ephemeral = true)
        draftService.startDraft(e.name, iData.tc, e.switchdraft)
    }
}
