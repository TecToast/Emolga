package de.tectoast.emolga.features.flo.replaywithguild

import de.tectoast.emolga.domain.game.service.ReplayWithGuildService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.toK18nMessageSender
import de.tectoast.emolga.features.interaction.toMessageSender
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ReplayWithGuildModal(private val service: ReplayWithGuildService) :
    ModalFeature<ReplayWithGuildModal.Args>(::Args, ModalSpec("rwithguild")) {
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
        val urls = e.urls.split("\n")
        when (val result = service.analyseForGuild(
            urls,
            iData.gid,
            id,
            iData.tc,
            iData.toK18nMessageSender(true),
            iData.toMessageSender(false)
        )) {
            is CalcResult.Success<*> -> iData.replyRaw("Replays wurden analysiert!", ephemeral = true)
            is CalcResult.Error<*> -> iData.reply(result.message)
        }
    }
}