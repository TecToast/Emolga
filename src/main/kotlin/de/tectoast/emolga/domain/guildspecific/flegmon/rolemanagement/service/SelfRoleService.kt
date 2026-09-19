package de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.service

import de.tectoast.emolga.discord.JDAGuildMemberRepository
import de.tectoast.emolga.discord.OptionalJDA
import de.tectoast.emolga.discord.jdaOrNull
import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.repository.FlegmonRoleRepository
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single


@Single
class SelfRoleService(
    @Named("flegmon") private val flegmonJda: OptionalJDA,
    private val roleRepo: FlegmonRoleRepository
) {
    val memberRepo = flegmonJda.jdaOrNull?.let { JDAGuildMemberRepository(it) }
    suspend fun setNewSelfRoles(guildId: Long, userId: Long, newRoleCompIds: List<String>) {
        val (add, remove) = roleRepo.getRoles().partition { it.compId in newRoleCompIds }.toList()
            .map { it.map { r -> r.roleId } }
        memberRepo?.modifyRoles(guildId, userId, add, remove)
    }
}