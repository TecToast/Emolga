package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.domain.game.service.ReplayService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.toK18nMessageSender
import de.tectoast.emolga.features.interaction.toMessageSender
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ReplayCommand(private val replayService: ReplayService) : CommandFeature<ReplayCommand.Args>(
    ::Args,
    CommandSpec("replay", K18n_Replay.Help)
) {

    class Args : Arguments() {
        var url by string("Replay-Link", K18n_Replay.ArgReplay)
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val result = replayService.analyseReplay(
            guild = iData.gid,
            tcId = iData.tc,
            urlProvided = e.url,
            infoSender = iData.toK18nMessageSender(true),
            replaySender = iData.toMessageSender(false)
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }
}

