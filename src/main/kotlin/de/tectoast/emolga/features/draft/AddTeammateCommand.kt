package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get

object AddTeammateCommand : CommandFeature<AddTeammateCommand.Args>(
    ::Args, CommandSpec(
        "addteammate",
        "Fügt einen Spieler zu deinem Team hinzu, falls du angemeldet bist",
        665600405136211989,
        Constants.G.FLP
    )
) {
    class Args : Arguments() {
        var user by member("User", "Der Spieler, den du hinzufügen möchtest")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val lsData = db.signups.get(gid)
        val uid = user
        val data = lsData?.users?.get(uid) ?: return reply("Du bist nicht angemeldet!")
        val member = e.user
        data.teammates += member.idLong
        reply("Du hast ${member.asMention} zu deinem Team hinzugefügt!", ephemeral = true)
        lsData.giveParticipantRole(member)
        jda.getTextChannelById(lsData.signupChannel)!!.editMessageById(data.signupmid!!, data.toMessage(uid, lsData))
            .queue()
        lsData.save()
    }
}
