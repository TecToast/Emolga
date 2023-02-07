package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.SignUpData
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SignupModal : ModalListener("signup") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val teamname = e.getValue("teamname")!!.asString
        val sdname = e.getValue("sdname")!!.asString
        with(Emolga.get.signups[e.guild!!.idLong]!!) {
            users[e.user.idLong] = SignUpData(teamname, sdname)
            if (users.size >= maxUsers) {
                e.jda.getTextChannelById(announceChannel)!!.let {
                    it.editMessageComponentsById(
                        announceMessageId,
                        primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
                    ).queue()
                    it.sendMessage("Anmeldung geschlossen!").queue()
                }
            }
            e.reply("Du wurdest erfolgreich angemeldet!").setEphemeral(true).queue()
            saveEmolgaJSON()
            e.jda.getTextChannelById(signupChannel)!!.sendMessage(
                "Anmeldung von <@${e.user.idLong}>:\n" +
                        "Teamname: **$teamname**\n" +
                        "Showdown-Name: **$sdname**"
            ).queue()
        }
    }
}
