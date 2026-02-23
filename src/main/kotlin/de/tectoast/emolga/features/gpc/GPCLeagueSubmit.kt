package de.tectoast.emolga.features.gpc

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji

object GPCLeagueSubmit {
    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("gpcleaguesubmit")) {
        override val label = "Liga registrieren".k18n
        override val emoji = Emoji.fromUnicode("\uD83D\uDCE9")

        class Args : Arguments() {
            var catId by long().compIdOnly()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.replyModal(Modal { this.catId = e.catId })
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("gpcleaguesubmitmodal")) {
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
            val category = iData.jda.getCategoryById(e.catId) ?: return iData.reply(
                "Es liegt eine Fehlkonfiguration vor, melde dich bitte bei ${Constants.MYTAG}!", ephemeral = true
            )
            val uid = iData.user
            val viewChannel = listOf(Permission.VIEW_CHANNEL)
            val empty = emptyList<Permission>()
            val tc = category.createTextChannel(e.name.take(100)).addRolePermissionOverride(
                1140764948914524301, viewChannel, empty
            ).addMemberPermissionOverride(
                uid, viewChannel, empty
            ).addMemberPermissionOverride(iData.jda.selfUser.idLong, viewChannel, empty).addRolePermissionOverride(
                1138451578483921046, empty, viewChannel
            ).await()
            tc.sendMessage("**${e.name}** (<@$uid>)\n**Doc-Link:** ${e.docUrl}\n\n**Infos zum Meta:**\n```${e.metaInfos.ifBlank { " " }}```\n**Sonstige Infos:**\n```${e.otherInfos.ifBlank { " " }}```")
                .await()
            iData.reply("Es wurde ein Kanal für die Registrierung deiner Liga erstellt: ${tc.asMention}")
        }
    }
}