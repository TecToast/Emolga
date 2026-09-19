package de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.service

import de.tectoast.emolga.discord.JDAGuildMemberRepository
import de.tectoast.emolga.discord.OptionalJDA
import de.tectoast.emolga.discord.jdaOrNull
import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.model.AcceptRulesResult
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

private const val ACCEPTED_RULES_ROLE = 605635673885507614

@Single
class RuleAcceptService(@Named("flegmon") private val flegmonJda: OptionalJDA) {
    val memberRepo = flegmonJda.jdaOrNull?.let { JDAGuildMemberRepository(it) }
    suspend fun acceptRules(guildId: Long, userId: Long, currentRoleIds: List<Long>): AcceptRulesResult {
        if (ACCEPTED_RULES_ROLE in currentRoleIds) return AcceptRulesResult.AlreadyAccepted
        memberRepo?.addRole(guildId, userId, ACCEPTED_RULES_ROLE)
        return AcceptRulesResult.Accepted
    }
}