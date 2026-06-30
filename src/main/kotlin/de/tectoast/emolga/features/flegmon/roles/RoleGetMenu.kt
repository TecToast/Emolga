package de.tectoast.emolga.features.flegmon.roles

import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.service.SelfRoleService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.StringSelectMenuFeature
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RoleGetMenu(private val service: SelfRoleService) :
    StringSelectMenuFeature<RoleGetMenu.Args>(::Args, SelectMenuSpec("roleget")) {


    class Args : Arguments() {
        var selection by multiOption(0..25)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        service.setNewSelfRoles(iData.gid, iData.user, e.selection)
        iData.replyRaw("Deine Rollen wurden angepasst!", ephemeral = true)
    }
}


