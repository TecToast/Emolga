package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.showdown.Analysis

object MultiReplay {
    object Command : CommandFeature<Command.Args>(
        ::Args,
        CommandSpec("multireplay", "Sendet mehrere Replays auf einmal!".k18n)
    ) {
        class Args : Arguments() {
            var replay by long("Replaychannel", "Replaychannel".k18n)
            var result by long("Resultchannel", "Resultchannel".k18n)
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
