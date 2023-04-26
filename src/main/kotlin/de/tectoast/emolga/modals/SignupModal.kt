package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.SignUpData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SignupModal : ModalListener("signup") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val teamname = e.getValue("teamname")!!.asString
        val sdname = e.getValue("sdname")!!.asString
        with(Emolga.get.signups[e.guild!!.idLong]!!) {
            if (name == "change") {
                val data = users[e.user.idLong]!!
                data.sdname = sdname
                data.teamname = teamname
                e.reply("Deine Daten wurden erfolgreich geändert!").setEphemeral(true).queue()
                e.jda.getTextChannelById(signupChannel)!!.editMessageById(
                    data.signupmid!!, "Anmeldung von <@${e.user.idLong}>:\n" +
                            "Teamname: **$teamname**\n" +
                            "Showdown-Name: **$sdname**"
                ).queue()
                data.logomid?.let {
                    e.jda.getTextChannelById(logoChannel)!!
                        .editMessageById(it, "**Logo von <@${e.user.idLong}> ($teamname):**").queue()
                }
                saveEmolgaJSON()
                SDNamesDB.addIfAbsent(sdname, e.user.idLong)
                return
            }
            val announceChannel = e.jda.getTextChannelById(announceChannel)!!
            val msg = announceChannel.retrieveMessageById(announceMessageId)
                .await().contentRaw.substringBefore("**Teilnehmer:")
            announceChannel.editMessageById(announceMessageId, "$msg**Teilnehmer: ${users.size}/$maxUsers**").queue()
            e.reply("Du wurdest erfolgreich angemeldet!").setEphemeral(true).queue()
            users[e.user.idLong] = SignUpData(
                teamname, sdname, e.jda.getTextChannelById(signupChannel)!!.sendMessage(
                    "Anmeldung von <@${e.user.idLong}>:\n" +
                            "Teamname: **$teamname**\n" +
                            "Showdown-Name: **$sdname**"
                ).await().idLong
            )
            if (users.size >= maxUsers) {
                announceChannel.editMessageComponentsById(
                    announceMessageId,
                    primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
                ).queue()
                announceChannel.sendMessage("Anmeldung geschlossen!").queue()
            }
            saveEmolgaJSON()
        }
    }
}
