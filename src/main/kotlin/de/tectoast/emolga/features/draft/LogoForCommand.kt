package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData

object LogoForCommand :
    CommandFeature<LogoForCommand.Args>(
        ::Args,
        CommandSpec(
            "logofor", "Reicht ein Logo für jemanden ein"
        )
    ) {

    init {
        restrict(roles(702233714360582154))
    }

    class Args : Arguments() {
        var user by member("User", "Der User")
        var logo by attachment("Logo", "Das Logo")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        LogoCommand.insertLogo(e.logo, e.user.idLong)
    }
}
