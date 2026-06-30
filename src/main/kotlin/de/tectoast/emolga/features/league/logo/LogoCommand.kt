package de.tectoast.emolga.features.league.logo

import de.tectoast.emolga.discord.toFileSubmission
import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Logo
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LogoCommand(private val signupService: SignupService) : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        K18n_Logo.Help,
    )
) {

    class Args : Arguments() {
        var logo by attachment("Logo", K18n_Logo.ArgLogo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.reply(signupService.insertLogo(iData.gid, iData.user, e.logo.toFileSubmission()).msg(), ephemeral = true)
    }

}
