package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.model.core.SwitchInput
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
class SwitchCommand(private val draftService: DraftService) :
    CommandFeature<SwitchCommand.Args>(::Args, CommandSpec("switch", K18n_Switch.Help)) {

    class Args : Arguments() {
        var oldmon by draftPokemon(
            "Altes Mon",
            K18n_Switch.ArgOldMon,
            autocomplete = { query, event ->
                val gid = event.guild?.idLong ?: return@draftPokemon null
                monOfTeam(query, gid, event.channelIdLong, event.user.idLong)
            }
        )
        var newmon by draftPokemon("Neues Mon", K18n_Switch.ArgNewMon)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val result = draftService.handleInputRequest(
            SwitchInput(e.oldmon, e.newmon),
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
