package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.model.core.BanInput
import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.validationCompleteCallback
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class BanMonCommand(private val draftService: DraftService) : CommandFeature<BanMonCommand.Args>(
    ::Args,
    CommandSpec("banmon", K18n_BanMon.Help)
) {
    class Args : Arguments() {
        var pokemon by draftPokemon("Pokemon", K18n_BanMon.ArgPokemon)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val result = draftService.handleInputRequest(
            BanInput(e.pokemon),
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
