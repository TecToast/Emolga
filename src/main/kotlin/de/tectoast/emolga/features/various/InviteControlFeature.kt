package de.tectoast.emolga.features.various

import de.tectoast.emolga.discord.GuildInviteCreator
import de.tectoast.emolga.domain.moderation.invitecontrol.repository.InviteControlRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class InviteControlFeature(
    private val inviteControlRepo: InviteControlRepository,
    private val guildInviteCreator: GuildInviteCreator
) :
    CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("invite", "Erstellt einen einmalig nutzbaren Invite".k18n)
    ) {
    init {
        registerListener<RoleCreateEvent> {
            val controlledGuilds = inviteControlRepo.guilds()
            if (it.guild.idLong in controlledGuilds) it.role.manager.revokePermissions(Permission.CREATE_INSTANT_INVITE)
                .queue()
        }
        registerListener<GuildInviteCreateEvent> { e ->
            val controlledGuilds = inviteControlRepo.guilds()
            val g = e.guild
            val adminRole = controlledGuilds[g.idLong] ?: return@registerListener
            g.retrieveMember(e.invite.inviter!!).queue {
                if (it.roles.any { r -> r.idLong == adminRole }) return@queue
                e.invite.delete().queue()
            }
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        guildInviteCreator.createOneTimeInvite(iData.gid)?.let { iData.replyRaw(it, ephemeral = true) }
            ?: iData.replyRaw("Error", ephemeral = true)
    }
}
