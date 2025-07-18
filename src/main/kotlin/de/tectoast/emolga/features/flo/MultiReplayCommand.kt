package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.showdown.Analysis

object MultiReplay {
    object Command : CommandFeature<Command.Args>(
        ::Args,
        CommandSpec("multireplay", "Sendet mehrere Replays auf einmal!")
    ) {
        class Args : Arguments() {
            var replay by long("Replaychannel", "Replaychannel")
            var result by long("Resultchannel", "Resultchannel")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.replyModal(Modal {
                replay = e.replay
                result = e.result
            })
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("multireplay")) {
        override val title = "Multi-Replay"

        class Args : Arguments() {
            var replay by long("Replaychannel", "Replaychannel").compIdOnly()
            var result by long("Resultchannel", "Resultchannel").compIdOnly()
            var replayLinks by string("ReplayLinks", "ReplayLinks") {
                modal(short = false)
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val replayChannel = iData.jda.getTextChannelById(e.replay)!!
            val resultChannel = iData.jda.getTextChannelById(e.result)!!
            val allReplays = e.replayLinks.split("\n")
            val lastIndex = allReplays.lastIndex
            iData.reply("Replays werden analysiert!", ephemeral = true)
            allReplays.forEachIndexed { index, url ->
                Analysis.analyseReplay(
                    urlProvided = url,
                    customReplayChannel = replayChannel,
                    resultchannelParam = resultChannel,
                    customGuild = replayChannel.guild.idLong,
                    withSort = index == lastIndex
                )
            }
        }
    }
}
