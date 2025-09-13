package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get

object AddTeammateCommand : CommandFeature<AddTeammateCommand.Args>(
    ::Args, CommandSpec(
        "addteammate",
        "Fügt einen Spieler zu deinem Team hinzu, falls du angemeldet bist",
    )
) {
    class Args : Arguments() {
        var user by member("User", "Der Spieler, den du hinzufügen möchtest")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val lsData =
            db.signups.get(iData.gid) ?: return iData.reply(
                "Es läuft derzeit keine Anmeldung auf diesem Server!",
                ephemeral = true
            )
        val data = lsData.getDataByUser(iData.user) ?: return iData.reply("Du bist nicht angemeldet!")
        val member = e.user
        if (lsData.getDataByUser(member.idLong) != null) return iData.reply("${member.asMention} ist bereits angemeldet!")
        lsData.handleNewUserInTeam(member, data)
        iData.reply("Du hast ${member.asMention} zu deinem Team hinzugefügt!", ephemeral = true)
    }
}
