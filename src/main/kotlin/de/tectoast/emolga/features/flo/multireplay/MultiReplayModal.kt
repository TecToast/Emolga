package de.tectoast.emolga.features.flo.multireplay

import de.tectoast.emolga.domain.game.service.MultiReplayService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.toK18nMessageSender
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import de.tectoast.emolga.utils.isError
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class MultiReplayModal(
    private val service: MultiReplayService
) :
    ModalFeature<MultiReplayModal.Args>(::Args, ModalSpec("multireplay")) {
    override val title = "Multi-Replay".k18n

    class Args : Arguments() {
        var replay by long("Replaychannel", "Replaychannel".k18n).compIdOnly()
        var result by long("Resultchannel", "Resultchannel".k18n).compIdOnly()
        var replayLinks by string("ReplayLinks", "ReplayLinks".k18n) {
            modal(short = false)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw("Replays werden analysiert!", ephemeral = true)
        val result =
            service.analyseMultiple(e.replay, e.result, e.replayLinks.split("\n"), iData.toK18nMessageSender(true))
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }
}