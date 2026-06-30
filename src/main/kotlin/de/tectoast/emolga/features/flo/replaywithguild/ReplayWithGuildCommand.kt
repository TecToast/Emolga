package de.tectoast.emolga.features.flo.replaywithguild

import de.tectoast.emolga.domain.game.service.ReplayWithGuildService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.toK18nMessageSender
import de.tectoast.emolga.features.interaction.toMessageSender
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ReplayWithGuildCommand(private val modal: ReplayWithGuildModal, private val service: ReplayWithGuildService) :
    CommandFeature<ReplayWithGuildCommand.Args>(::Args, CommandSpec("rwithguild", "Replay with guild".k18n)) {
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
            return iData.replyModal(modal())
        }
        iData.deferReply()
        val result = service.analyseForGuild(
            e.urls,
            iData.gid,
            e.guild,
            iData.tc,
            iData.toK18nMessageSender(true),
            iData.toMessageSender(false)
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }
}