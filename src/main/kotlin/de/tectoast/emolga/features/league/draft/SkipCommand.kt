package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.validationCompleteCallback
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SkipCommand(private val draftService: DraftService) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("skip", K18n_Skip.Help)) {

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        val result = draftService.handleSkipRequest(
            iData.tc,
            iData.user,
            iData.data.memberRoles,
            iData.validationCompleteCallback
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }
}
