package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.SignUpData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SignupModal : ModalListener("signup") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val teamname = e.getValue("teamname")?.asString
        val sdname = e.getValue("sdname")!!.asString
        val sdnameid = Command.toUsername(sdname)
        if (sdnameid.length !in 1..18) return e.reply_("Dieser Showdown-Name ist ungültig!").setEphemeral(true).queue()
        val uid = e.user.idLong
        val isChange = name == "change"
        val g = e.guild!!
        with(db.signups.get(g.idLong)!!) {
            val ownerOfTeam =
                PrivateCommands.userIdForSignupChange?.takeIf { uid == Constants.FLOID }
                    ?: if (isChange) getOwnerByUser(uid) ?: return e.reply_(
                        "Du bist derzeit nicht angemeldet!"
                    ).setEphemeral(true).queue()
                    else uid

            SDNamesDB.addIfAbsent(sdname, ownerOfTeam)
            if (isChange) {
                val data = users[ownerOfTeam]!!
                data.sdname = sdname
                data.teamname = teamname
                e.reply("Deine Daten wurden erfolgreich geändert!").setEphemeral(true).queue()
                e.jda.getTextChannelById(signupChannel)!!.editMessageById(
                    data.signupmid!!, data.toMessage(ownerOfTeam, this)
                ).queue()
                data.logomid?.let {
                    e.jda.getTextChannelById(logoChannel)!!
                        .editMessageById(it, "**Logo von <@$ownerOfTeam> ($teamname):**").queue()
                }
                save()
                return
            }
            val announceChannel = e.jda.getTextChannelById(announceChannel)!!
            e.reply("Du wurdest erfolgreich angemeldet!").setEphemeral(true).queue()
            giveParticipantRole(e.member!!)
            val signUpData = SignUpData(
                teamname, sdname,
            ).apply {
                signupmid = e.jda.getTextChannelById(signupChannel)!!.sendMessage(
                    toMessage(ownerOfTeam, this@with)
                ).await().idLong
            }
            users[ownerOfTeam] = signUpData
            val msg = announceChannel.retrieveMessageById(announceMessageId)
                .await().contentRaw.substringBefore("**Teilnehmer:")
            announceChannel.editMessageById(announceMessageId, "$msg**Teilnehmer: ${users.size}/${maxUsersAsString}**")
                .queue()
            if (maxUsers > 0 && users.size >= maxUsers) {
                announceChannel.editMessageComponentsById(
                    announceMessageId, primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
                ).queue()
                announceChannel.sendMessage("_----------- Anmeldung geschlossen -----------_").queue()
            }
            save()
        }
    }

    companion object {
        fun getModal(data: SignUpData?, lsData: LigaStartData) =
            Modal("signup".condAppend(data != null, ";change"), "Anmeldung".condAppend(data != null, "sanpassung")) {
                if (!lsData.noTeam)
                    short("teamname", "Team-Name", required = true, value = data?.teamname)
                short("sdname", "Showdown-Name", required = true, requiredLength = 1..18, value = data?.sdname)
            }
    }
}
