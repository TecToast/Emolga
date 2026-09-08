package de.tectoast.emolga.domain.league.signup.service

import de.tectoast.emolga.domain.league.signup.model.data.ParticipantData
import de.tectoast.emolga.domain.league.signup.model.data.ParticipantDataGet
import de.tectoast.emolga.domain.league.signup.model.data.ParticipantDataSet
import de.tectoast.emolga.domain.league.signup.model.data.UserData
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import org.koin.core.annotation.Single

@Single
class SignupParticipantService(
    private val signupRepo: SignupRepository,
    private val discordUserService: DiscordUserService,
) {
    suspend fun getParticipantsForSignup(guild: Long, identifier: String): ParticipantDataGet? {
        val signup = signupRepo.getLeagueSignup(guild, identifier) ?: return null
        val allEntries = signupRepo.getAllEntries(signup.id)
        val relevantUsers = allEntries.values.flatMap {
            it.users + it.data.entries.filter { (k, _) -> k.startsWith("user_") }
                .mapNotNull { (_, v) -> v.toLongOrNull() }
        }
        val userData = discordUserService.getData(guild, relevantUsers)
        val result = allEntries.map { (id, it) ->
            ParticipantData(
                id = id,
                users = it.users.map { u ->
                    val data = userData[u]
                    UserData(u.toString(), data?.displayName ?: "N/A", data?.avatarUrl ?: "")
                },
                data = it.data.mapValues { (k, v) ->
                    if (k.startsWith("user_")) {
                        val userId = v.toLongOrNull() ?: return@mapValues v
                        val data = userData[userId]
                        data?.displayName ?: v
                    } else {
                        v
                    }
                },
                hasLogo = it.logoIdentifier != null,
                conference = it.conference,
            )
        }
        return ParticipantDataGet(signup.conferences, result)
    }

    suspend fun setConferences(guild: Long, identifier: String, setData: ParticipantDataSet): Unit? {
        val signup = signupRepo.getLeagueSignup(guild, identifier) ?: return null
        signupRepo.setConferencesForSignup(signup.id, setData.conferences)
        signupRepo.setConferencesForEntries(signup.id, setData.data)
        return Unit
    }
}