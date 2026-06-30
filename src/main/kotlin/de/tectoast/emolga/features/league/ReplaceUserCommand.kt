package de.tectoast.emolga.features.league

import de.tectoast.emolga.domain.league.member.service.UserReplaceService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ReplaceUserCommand(private val service: UserReplaceService) :
    CommandFeature<ReplaceUserCommand.Args>(::Args, CommandSpec("replaceuser", K18n_ReplaceUser.Help)) {

    init {
        restrict(flo)
    }

    class Args : Arguments() {
        val oldUser by member("OldUser", K18n_ReplaceUser.ArgOldUser)
        val newUser by member("NewUser", K18n_ReplaceUser.ArgNewUser)
        val sdName by string("SDName", K18n_ReplaceUser.ArgSDName).nullable()
        val teamName by string("TeamName", K18n_ReplaceUser.ArgTeamName).nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val result = service.replaceUser(iData.gid, e.oldUser.idLong, e.newUser.idLong, e.sdName, e.teamName)
        if (result) {
            iData.done(true)
        } else {
            iData.reply(K18n_ReplaceUser.OldUserNotInLeague, ephemeral = true)
        }
    }
}

