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
class DSBTextCommand(private val service: DSBSubmissionService) :
    CommandFeature<DSBTextCommand.Args>(::Args, CommandSpec("dsbtext", "dsbtext".k18n)) {
    class Args : Arguments() {
        val text by string("text", "text".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        service.handleTextSubmission(iData.user, e.text)
        iData.done(true)
    }
}
