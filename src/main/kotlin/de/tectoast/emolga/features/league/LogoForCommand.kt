package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData

object LogoForCommand :
    CommandFeature<LogoForCommand.Args>(
        ::Args,
        CommandSpec(
            "logofor", K18n_LogoFor.Help
        )
    ) {

    init {
        restrict(roles(702233714360582154))
    }

    class Args : Arguments() {
        var user by member("User", K18n_LogoFor.ArgUser)
        var logo by attachment("Logo", K18n_LogoFor.ArgLogo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        LogoCommand.insertLogo(e.logo, e.user.idLong)
    }
}
