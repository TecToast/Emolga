package de.tectoast.emolga.features.league.logo

import de.tectoast.emolga.discord.toFileSubmission
import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_LogoFor
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LogoForCommand(private val signupService: SignupService) :
    CommandFeature<LogoForCommand.Args>(
        ::Args,
        CommandSpec(
            "logofor", K18n_LogoFor.Help
        )
    ) {

    init {
        restrict(admin)
    }

    class Args : Arguments() {
        var user by member("User", K18n_LogoFor.ArgUser)
        var logo by attachment("Logo", K18n_LogoFor.ArgLogo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(true)
        iData.reply(
            signupService.insertLogo(iData.gid, e.user.idLong, e.logo.toFileSubmission()).msg(),
            ephemeral = true
        )
    }
}
