package de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.service

import de.tectoast.emolga.discord.GuildMemberRepository
import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.repository.FlegmonRoleRepository
import org.koin.core.annotation.Single


@Single
class SelfRoleService(private val memberRepo: GuildMemberRepository, private val roleRepo: FlegmonRoleRepository) {
    suspend fun setNewSelfRoles(guildId: Long, userId: Long, newRoleCompIds: List<String>) {
        val (add, remove) = roleRepo.getRoles().partition { it.compId in newRoleCompIds }.toList()
            .map { it.map { r -> r.roleId } }
        memberRepo.modifyRoles(guildId, userId, add, remove)
    }
}