package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.interactions.components.Modal

class SignUpChangeCommand :
    Command("signupchange", "Ermöglicht es dir, deine Anmeldung anzupassen", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.ASL, Constants.G.FLP, 665600405136211989, Constants.G.WFS)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val signups = db.signups.get(e.guild.idLong) ?: return e.reply(
            "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
        )
        val uid = e.author.idLong
        val signUpData =
            signups.users[PrivateCommands.userIdForSignupChange?.takeIf { uid == Constants.FLOID } ?: uid]
                ?: return e.reply("Du bist derzeit nicht angemeldet!", ephemeral = true)
        e.slashCommandEvent!!.replyModal(Modal("signup;change", "Anmeldungsanpassung") {
            short("teamname", "Team-Name", required = true, value = signUpData.teamname)
            short("sdname", "Showdown-Name", required = true, value = signUpData.sdname)
        }).queue()
    }
}
