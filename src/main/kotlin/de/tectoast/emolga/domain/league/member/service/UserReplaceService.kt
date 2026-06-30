package de.tectoast.emolga.domain.league.member.service

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.domain.league.teamgraphic.service.TeamGraphicGenerator
import org.koin.core.annotation.Single

@Single
class UserReplaceService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueSignupRepo: SignupRepository,
    private val teamGraphicGenerator: TeamGraphicGenerator
) {
    suspend fun replaceUser(
        guild: Long,
        oldUser: Long,
        newUser: Long,
        newSDName: String?,
        newTeamName: String?
    ): Boolean {
        val (leagueName, idx) = leagueCoreRepo.getLeagueNameAndIdxByGuildUser(guild, oldUser) ?: return false
        leagueMemberRepo.replaceUser(leagueName, oldUser, newUser)
        val signup = leagueSignupRepo.getLeagueSignupOfUser(guild, oldUser) ?: return true
        val (entryId, entry) = leagueSignupRepo.getSignupEntryByUserId(signup.id, oldUser) ?: return true
        entry.users.apply {
            remove(oldUser)
            add(newUser)
        }
        newSDName?.let {
            entry.data[SignupInput.SDNAME_ID] = it
        }
        newTeamName?.let {
            entry.data[SignupInput.TEAMNAME_ID] = it
        } ?: teamGraphicGenerator.editTeamGraphicForLeague(leagueName, idx)
        leagueSignupRepo.editSignupEntry(entryId, entry)
        return true
    }
}
