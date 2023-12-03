package de.tectoast.emolga.modals

import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object ShinyModal : ModalListener("shiny") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val uid = name!!.toLong()
        val reason = e.getValue("reason")?.asString ?: "_Kein Grund angegeben_"
        val url = e.message!!.contentRaw.substringAfterLast(": ")
        e.reply_("Deine Begründung wurde an den User gesendet!").setEphemeral(true).queue()
        e.jda.openPrivateChannelById(uid)
            .flatMap {
                it.sendMessage(
                    "Vielen Dank, dass du das Shiny eingereicht hast. Leider können wir das Shiny unter folgendem Grund nicht berücksichtigen: **$reason**\n\nBild-URL: $url"
                )
            }.queue()
        e.message!!.delete().queue()
    }

}
