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
class ReplayBo3Command(private val replayService: ReplayService) :
    CommandFeature<ReplayBo3Command.Args>(::Args, CommandSpec("replaybo3", K18n_ReplayBo3.Help)) {
    class Args : Arguments() {
        var replay1 by string("Replay 1", K18n_ReplayBo3.ArgReplay1)
        var replay2 by string("Replay 2", K18n_ReplayBo3.ArgReplay2)
        var replay3 by string("Replay 3", K18n_ReplayBo3.ArgReplay3).nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val result = replayService.analyseBo3(
            iData.gid,
            iData.tc,
            listOfNotNull(e.replay1, e.replay2, e.replay3),
            iData.toK18nMessageSender(true),
            iData.toMessageSender(false)
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }
}
