package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.showdown.Analysis

object RWithGuild {
    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("rwithguild", "Replay with guild")) {
        init {
            restrict(flo)
        }

        class Args : Arguments() {
            var guild by long("gid", "gid")
            var url by string("url", "url")
        }

        context(InteractionData) override suspend fun exec(e: Args) = slashEvent {
            val url = e.url
            if (url == "-") {
                return replyModal(Modal()).queue()
            }
            deferReply()
            Analysis.analyseReplay(
                url,
                resultchannelParam = textChannel,
                customGuild = e.guild,
                fromReplayCommand = self
            )
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("rwithguild")) {
        override val title = "Replays mit Guild"

        class Args : Arguments() {
            var id by long("Guild-ID", "Gid")
            var urls by string("URLs", "URLs") {
                modal(false)
            }
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            val id = e.id
            val urls = e.urls
            urls.split("\n").forEach {
                Analysis.analyseReplay(
                    it,
                    resultchannelParam = textChannel,
                    customGuild = id
                )
            }
            reply("Replays wurden analysiert!", ephemeral = true)
        }
    }
}
