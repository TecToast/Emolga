package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent

object ASLInviteControl : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec("invite", "Erstellt einen einmalig nutzbaren Invite", Constants.G.ASL)
) {
    init {
        restrict(roles(702233714360582154))
        registerListener<RoleCreateEvent> {
            when (it.guild.idLong) {
                Constants.G.ASL, Constants.G.FLP -> it.role.manager.revokePermissions(Permission.CREATE_INSTANT_INVITE)
                    .queue()
            }
        }
        registerListener<GuildInviteCreateEvent> { e ->
            val g = e.guild
            if (g.idLong == Constants.G.ASL) {
                g.retrieveMember(e.invite.inviter!!).queue {
                    if (g.selfMember.canInteract(it)) e.invite.delete().queue()
                }
            }
        }
    }

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        guild().defaultChannel?.createInvite()?.setMaxUses(1)?.map { reply(it.url, ephemeral = true) }
            ?.queue() ?: reply("Kein Default Channel gefunden! Sollte nicht passieren, melde dich bei Flo")
    }
}
