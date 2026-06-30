package de.tectoast.emolga.features.league

import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class AddTeammateCommand(private val signupService: SignupService) : CommandFeature<AddTeammateCommand.Args>(
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
        val result = signupService.addTeammate(iData.gid, iData.user, e.user.idLong)
        iData.reply(result.msg(), ephemeral = true)
    }
}
