package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class SignupButton : ButtonListener("signup") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val gid = e.guild?.idLong
        if (gid !in Emolga.get.signups) {
            e.reply("Diese Anmeldung ist bereits geschlossen!").setEphemeral(true).queue()
            return
        }
        val signups = Emolga.get.signups[gid]!!
        if (e.user.idLong in signups.users) {
            e.reply("Du bist bereits angemeldet!").setEphemeral(true).queue()
            return
        }
        e.replyModal(Modal("signup", "Anmeldung") {
            short("teamname", "Team-Name", required = true)
            short("sdname", "Showdown-Name", required = true)
        }).queue()
    }
}