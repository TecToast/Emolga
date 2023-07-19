package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.Constants
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
            val uid =
                PrivateCommands.userIdForSignupChange?.takeIf { e.user.idLong == Constants.FLOID } ?: e.user.idLong
            if (name == "change") {
                val data = users[uid]!!
                data.sdname = sdname
                data.teamname = teamname
                e.reply("Deine Daten wurden erfolgreich geändert!").setEphemeral(true).queue()
                e.jda.getTextChannelById(signupChannel)!!.editMessageById(
                    data.signupmid!!, data.toMessage(uid)
                ).queue()
                data.logomid?.let {
                    e.jda.getTextChannelById(logoChannel)!!
                        .editMessageById(it, "**Logo von <@$uid> ($teamname):**").queue()
                }
                saveEmolgaJSON()
                SDNamesDB.addIfAbsent(sdname, uid)
                return
            }
            val announceChannel = e.jda.getTextChannelById(announceChannel)!!
            e.reply("Du wurdest erfolgreich angemeldet!").setEphemeral(true).queue()
            val signUpData = SignUpData(
                teamname, sdname,
            ).apply {
                signupmid = e.jda.getTextChannelById(signupChannel)!!.sendMessage(
                    toMessage(uid)
                ).await().idLong
            }
            users[uid] = signUpData
            val msg = announceChannel.retrieveMessageById(announceMessageId)
                .await().contentRaw.substringBefore("**Teilnehmer:")
            announceChannel.editMessageById(announceMessageId, "$msg**Teilnehmer: ${users.size}/$maxUsers**").queue()
            if (users.size >= maxUsers) {
                announceChannel.editMessageComponentsById(
                    announceMessageId,
                    primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
                ).queue()
                announceChannel.sendMessage("_----------- Anmeldung geschlossen -----------_").queue()
            }
            saveEmolgaJSON()
        }
    }
}
