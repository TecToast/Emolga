package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.modals.SignupModal
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get

object SignUpChangeCommand :
    Command("signupchange", "Ermöglicht es dir, deine Anmeldung anzupassen", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(
            true,
            Constants.G.ASL,
            Constants.G.FLP,
            665600405136211989,
            Constants.G.WFS,
            Constants.G.ADK,
            Constants.G.NDS
        )
    }

    override suspend fun process(e: GuildCommandEvent) {
        val ligaStartData = db.signups.get(e.guild.idLong) ?: return e.reply(
            "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
        )
        val uid = e.author.idLong
        val signUpData =
            ligaStartData.users[PrivateCommands.userIdForSignupChange?.takeIf { uid == Constants.FLOID }]
                ?: ligaStartData.getDataByUser(uid)
                ?: return e.reply("Du bist derzeit nicht angemeldet!", ephemeral = true)
        e.slashCommandEvent!!.replyModal(SignupModal.getModal(signUpData, ligaStartData)).queue()
    }
}
