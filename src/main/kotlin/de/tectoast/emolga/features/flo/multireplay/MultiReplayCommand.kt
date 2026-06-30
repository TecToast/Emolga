package de.tectoast.emolga.features.flo.multireplay

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class MultiReplayCommand(private val modal: MultiReplayModal) : CommandFeature<MultiReplayCommand.Args>(
    ::Args,
    CommandSpec("multireplay", "Sendet mehrere Replays auf einmal!".k18n)
) {

    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var replay by long("Replaychannel", "Replaychannel".k18n)
        var result by long("Resultchannel", "Resultchannel".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyModal(modal {
            replay = e.replay
            result = e.result
        })
    }
}