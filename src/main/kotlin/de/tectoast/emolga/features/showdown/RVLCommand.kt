package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.generics.getChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

object RVLCommand :
    CommandFeature<RVLCommand.Args>(::Args, CommandSpec("rvl", "Replay-Command für die RVL", Constants.G.VIP)) {
    class Args : Arguments() {
        var replay1 by string("Replay 1", "Das erste Replay")
        var replay2 by string("Replay 2", "Das zweite Replay")
        var replay3 by string("Replay 3", "Das dritte Replay, falls es existiert").nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val channel = AnalysisDB.getResultChannel(iData.tc)
            ?: return iData.reply("Dieser Channel ist kein Replaychannel! Mit `/replaychannel add` kannst du diesen Channel zu einem Replaychannel machen!")
        val tc = iData.jda.getChannel<GuildMessageChannel>(channel)
        if (tc == null) {
            iData.reply("Ich habe keinen Zugriff auf den Ergebnischannel!")
            return
        }
        Analysis.analyseReplay(
            urlsProvided = listOfNotNull(e.replay1, e.replay2, e.replay3),
            resultchannelParam = tc,
            fromReplayCommand = iData,
            customGuild = Constants.G.VIP
        )
    }
}