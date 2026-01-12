package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message

object LogoCommand : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        "Reicht dein Logo ein",
    )
) {
    private val logger = KotlinLogging.logger {}
    val allowedFileFormats = setOf("png", "jpg", "jpeg", "webp")

    class Args : Arguments() {
        var logo by attachment("Logo", "Das Logo")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        insertLogo(e.logo, iData.user)
    }

    context(iData: InteractionData)
    suspend fun insertLogo(logo: Message.Attachment, uid: Long) {
        iData.deferReply(ephemeral = true)
        val lsData = db.signups.get(iData.gid)!!
        val error = lsData.insertLogo(uid, logo)
        iData.reply(error ?: "Dein Logo wurde erfolgreich hochgeladen!")
    }


}
