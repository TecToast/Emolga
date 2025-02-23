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
        *draftGuilds
    )
) {
    class Args : Arguments() {
        var user by member("User", "Der Spieler, den du hinzufügen möchtest")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val lsData =
            db.signups.get(gid) ?: return reply("Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true)
        val data = lsData.getDataByUser(user) ?: return reply("Du bist nicht angemeldet!")
        val member = e.user
        if (lsData.getDataByUser(member.idLong) != null) return reply("${member.asMention} ist bereits angemeldet!")
        lsData.handleNewUserInTeam(member, data)
        reply("Du hast ${member.asMention} zu deinem Team hinzugefügt!", ephemeral = true)
    }
}
