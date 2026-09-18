package de.tectoast.emolga.features.various

import de.tectoast.emolga.domain.league.admin.repository.GuildManagerRepository
import de.tectoast.emolga.domain.web.service.LoginService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LoginCommand(
    private val guildManagerRepo: GuildManagerRepository,
    private val service: LoginService,
) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("login", K18n_Login.Help)) {

    init {
        restrict {
            guildManagerRepo.isAuthorizedForLoginCommand(gid, user, data.memberRoles)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.reply(service.createLogin(iData.user, iData.gid), ephemeral = true)
    }
}