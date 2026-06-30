package de.tectoast.emolga.features.gpc

import de.tectoast.emolga.domain.guildspecific.gpc.service.GPCSubmissionHandler
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class GPCLeagueSubmitModal(private val gpcSubmissionHandler: GPCSubmissionHandler) :
    ModalFeature<GPCLeagueSubmitModal.Args>(::Args, ModalSpec("gpcleaguesubmitmodal")) {
    override val title = "Liga registrieren".k18n

    class Args : Arguments() {
        var catId by long().compIdOnly()
        var name by string("Name", "Name der Liga".k18n)
        var docUrl by string("Doc-Link", "Link zum Dokument mit den Teilnehmern".k18n)
        var metaInfos by string("Infos zum Meta", "Hier kannst du Infos zum gespielten Meta angeben".k18n) {
            modal(short = false)
        }
        var otherInfos by string(
            "Sonstige Infos", "Hier kannst du sonstige Infos angeben, die evtl. relevant sein könnten".k18n
        ) {
            modal(short = false)
        }.default("")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(true)
        iData.replyRaw(
            gpcSubmissionHandler.handle(
                uid = iData.user,
                catId = e.catId,
                name = e.name,
                docUrl = e.docUrl,
                metaInfos = e.metaInfos,
                otherInfos = e.otherInfos
            )
        )
    }
}