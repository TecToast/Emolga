package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message

object LogoCommand : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        K18n_Logo.Help,
    )
) {
    private val logger = KotlinLogging.logger {}
    val allowedFileFormats = setOf("png", "jpg", "jpeg", "webp")

    class Args : Arguments() {
        var logo by attachment("Logo", K18n_Logo.ArgLogo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        insertLogo(e.logo, iData.user)
    }

    context(iData: InteractionData)
    suspend fun insertLogo(logo: Message.Attachment, uid: Long) {
        iData.deferReply(ephemeral = true)
        val lsData = mdb.signups.get(iData.gid)!!
        val error = lsData.insertLogo(uid, logo)
        iData.reply(error ?: K18n_Logo.Success, ephemeral = true)
    }


}
