package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec

object LogoForCommand :
    CommandFeature<LogoForCommand.Args>(::Args, CommandSpec("logofor", "Reicht ein Logo für jemanden ein")) {
    class Args : Arguments() {
        var user by member("User", "Der User")
        var logo by attachment("Logo", "Das Logo")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        LogoCommand.insertLogo(e.logo, e.user.idLong)
    }
}
