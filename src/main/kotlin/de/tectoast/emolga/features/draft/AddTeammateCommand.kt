package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.during.generic.K18n_NoSignupInGuild
import de.tectoast.emolga.features.draft.during.generic.K18n_NotSignedUp
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb

object AddTeammateCommand : CommandFeature<AddTeammateCommand.Args>(
    ::Args, CommandSpec(
        "addteammate",
        K18n_AddTeammate.Help,
    )
) {
    class Args : Arguments() {
        var user by member("User", K18n_AddTeammate.ArgUser)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val lsData =
            mdb.signups.get(iData.gid) ?: return iData.reply(
                K18n_NoSignupInGuild,
                ephemeral = true
            )
        val data = lsData.getDataByUser(iData.user) ?: return iData.reply(K18n_NotSignedUp)
        val member = e.user
        if (lsData.getDataByUser(member.idLong) != null) return iData.reply(
            K18n_AddTeammate.PartnerAlreadySignedUp(
                member.idLong
            )
        )
        lsData.handleNewUserInTeam(member, data)
        iData.reply(K18n_AddTeammate.Success(member.idLong), ephemeral = true)
    }
}
