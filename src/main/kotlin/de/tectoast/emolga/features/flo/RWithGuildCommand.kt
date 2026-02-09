package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.showdown.Analysis

object RWithGuild {
    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("rwithguild", "Replay with guild".k18n)) {
        init {
            restrict(flo)
        }

        class Args : Arguments() {
            var guild by long("gid", "gid".k18n)
            var urls by list("url", "url".k18n, numOfArgs = 3, requiredNum = 1)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val url = e.urls.first()
            if (url == "-") {
                return iData.replyModal(Modal())
            }
            iData.deferReply()
            Analysis.analyseReplay(
                e.urls,
                resultchannelParam = iData.textChannel,
                customGuild = e.guild,
                fromReplayCommand = iData
            )
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("rwithguild")) {
        override val title = "Replays mit Guild".k18n

        class Args : Arguments() {
            var id by long("Guild-ID", "Gid".k18n)
            var urls by string("URLs", "URLs".k18n) {
                modal(false)
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val id = e.id
            val urls = e.urls
            urls.split("\n").forEach {
                Analysis.analyseReplay(
                    it,
                    resultchannelParam = iData.textChannel,
                    customGuild = id
                )
            }
            iData.reply("Replays wurden analysiert!", ephemeral = true)
        }
    }
}
