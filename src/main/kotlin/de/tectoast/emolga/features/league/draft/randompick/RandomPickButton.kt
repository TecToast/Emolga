package de.tectoast.emolga.features.league.draft.randompick

import de.tectoast.emolga.domain.league.draft.model.random.RandomPickAction
import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.validationCompleteCallback
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RandomPickButton(private val draftService: DraftService) :
    ButtonFeature<RandomPickButton.Args>(::Args, ButtonSpec("randompick")) {
    class Args : Arguments() {
        var action by enumBasic<RandomPickAction>()
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val result = draftService.handleRandomPickFollowUpRequest(
            action = e.action,
            tcId = iData.tc,
            userId = iData.user,
            roleIds = iData.data.memberRoles,
            validationCompleteCallback = iData.validationCompleteCallback
        )
        handleRandomPickResult(result, this)
    }
}