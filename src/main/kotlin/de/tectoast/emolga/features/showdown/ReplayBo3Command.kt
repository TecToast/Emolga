package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.generics.getChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

object ReplayBo3Command :
    CommandFeature<ReplayBo3Command.Args>(::Args, CommandSpec("replaybo3", K18n_ReplayBo3.Help)) {
    class Args : Arguments() {
        var replay1 by string("Replay 1", K18n_ReplayBo3.ArgReplay1)
        var replay2 by string("Replay 2", K18n_ReplayBo3.ArgReplay2)
        var replay3 by string("Replay 3", K18n_ReplayBo3.ArgReplay3).nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val channel = AnalysisDB.getResultChannel(iData.tc)
            ?: return iData.reply(K18n_ReplayGeneric.NoReplayChannel)
        val tc = iData.jda.getChannel<GuildMessageChannel>(channel)
            ?: return iData.reply(K18n_ReplayGeneric.NoAccessToResultChannel(channel))
        Analysis.analyseReplay(
            urlsProvided = listOfNotNull(e.replay1, e.replay2, e.replay3),
            resultchannelParam = tc,
            fromReplayCommand = iData,
        )
    }
}