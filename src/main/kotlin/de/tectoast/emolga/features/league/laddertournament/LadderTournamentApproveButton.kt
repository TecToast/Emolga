package de.tectoast.emolga.features.league.laddertournament

import de.tectoast.emolga.domain.guildspecific.laddertournament.service.LadderTournamentService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_LadderTournament
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.generic.K18n_Approve
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LadderTournamentApproveButton(private val service: LadderTournamentService) :
    ButtonFeature<LadderTournamentApproveButton.Args>(::Args, ButtonSpec("laddertournamentapprove")) {
    override val label = K18n_Approve

    class Args : Arguments() {
        var user by long()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        when (val result = service.handleApproveRequest(iData.gid, e.user)) {
            is CalcResult.Success<*> -> {
                iData.edit(contentK18n = null, components = emptyList())
                iData.reply(K18n_LadderTournament.Verified, ephemeral = true)
            }

            is CalcResult.Error<*> -> {
                iData.reply(result.message, ephemeral = true)
            }
        }

    }
}