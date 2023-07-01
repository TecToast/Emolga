package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class SignupButton : ButtonListener("signup") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val gid = e.guild!!.idLong
        val signups = db.signups.get(gid)
            ?: return e.reply("Diese Anmeldung ist bereits geschlossen!").setEphemeral(true).queue()
        val uid = e.user.idLong
        if (uid in signups.users || signups.users.values.any { uid in it.teammates }) {
            e.reply("Du bist bereits angemeldet!").setEphemeral(true).queue()
            return
        }
        e.replyModal(Modal("signup", "Anmeldung") {
            short("teamname", "Team-Name", required = true)
            short("sdname", "Showdown-Name", required = true)
        }).queue()
    }
}
