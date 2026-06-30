package de.tectoast.emolga.features.flegmon.dsb

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service.DSBSubmissionService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class DSBMonCommand(private val service: DSBSubmissionService) :
    CommandFeature<DSBMonCommand.Args>(::Args, CommandSpec("dsbmon", "dsbmon".k18n)) {
    class Args : Arguments() {
        val mon by draftPokemon("mon", "mon".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        service.handleMonSubmission(iData.user, e.mon)
        iData.done(true)
    }
}

