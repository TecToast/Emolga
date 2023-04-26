package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.interactions.components.Modal

class SignUpChangeCommand :
    Command("signupchange", "Ermöglicht es dir, deine Anmeldung anzupassen", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.ASL, Constants.G.FLP)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val signups = Emolga.get.signups[e.guild.idLong] ?: return e.reply(
            "Es läuft derzeit keine Anmeldung auf diesem Server!", ephemeral = true
        )
        val signUpData =
            signups.users[e.author.idLong] ?: return e.reply("Du bist derzeit nicht angemeldet!")
        e.slashCommandEvent!!.replyModal(Modal("signup;change", "Anmeldungsanpassung") {
            short("teamname", "Team-Name", required = true, value = signUpData.teamname)
            short("sdname", "Showdown-Name", required = true, value = signUpData.sdname)
        }).queue()
    }
}
