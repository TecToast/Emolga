package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.modals.SignupModal
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get

object SignupChangeCommand : CommandFeature<NoArgs>(
    NoArgs(), CommandSpec(
        "signupchange",
        "Ermöglicht es dir, deine Anmeldung anzupassen",
        Constants.G.ASL,
        Constants.G.FLP,
        665600405136211989,
        Constants.G.WFS,
        Constants.G.ADK,
        Constants.G.NDS
    )
) {
    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        val ligaStartData = db.signups.get(gid) ?: return reply(
            "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
        )
        val signUpData = ligaStartData.users[PrivateCommands.userIdForSignupChange?.takeIf { user == Constants.FLOID }]
            ?: ligaStartData.getDataByUser(user) ?: return reply("Du bist derzeit nicht angemeldet!", ephemeral = true)
        replyModal(SignupModal.getModal(signUpData, ligaStartData))
    }
}
