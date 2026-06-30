package de.tectoast.emolga.domain.league.admin.service

import de.tectoast.emolga.discord.GuildMetaRepository
import de.tectoast.emolga.domain.league.admin.model.GuildMeta
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicMetaRepository
import org.koin.core.annotation.Single

@Single
class GuildListService(
    private val guildAccessService: GuildAccessService,
    private val guildMetaRepo: GuildMetaRepository,
    private val signupRepo: SignupRepository,
    private val teamgraphicsMetaRepo: TeamGraphicMetaRepository
) {
    suspend fun getGuildsForUser(userId: Long): List<GuildMeta> {
        return guildAccessService.getGuildsForUser(userId).mapNotNull { guildId ->
            val name = guildMetaRepo.getGuildName(guildId) ?: return@mapNotNull null
            val icon = guildMetaRepo.getGuildIcon(guildId).orEmpty()
            val hasSignup = signupRepo.hasRunningSignup(guildId)
            val teamgraphicsShape = teamgraphicsMetaRepo.getShape(guildId)
            GuildMeta(
                id = guildId.toString(),
                name = name,
                icon = icon,
                runningSignup = hasSignup,
                teamgraphicShape = teamgraphicsShape
            )
        }
    }
}