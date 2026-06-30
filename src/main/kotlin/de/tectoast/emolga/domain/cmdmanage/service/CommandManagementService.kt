package de.tectoast.emolga.domain.cmdmanage.service

import de.tectoast.emolga.domain.cmdmanage.model.AddRemove
import de.tectoast.emolga.domain.cmdmanage.model.AddRemove.ADD
import de.tectoast.emolga.domain.cmdmanage.model.AddRemove.REMOVE
import de.tectoast.emolga.domain.cmdmanage.repository.CommandManagementRepository
import de.tectoast.emolga.utils.BotConstants
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single
class CommandManagementService(
    private val repo: CommandManagementRepository,
    private val botConstants: BotConstants
) {

    private val logger = KotlinLogging.logger {}

    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove) {
        when (action) {
            ADD -> repo.addGuildGroup(guildId, group)
            REMOVE -> repo.removeGuildGroup(guildId, group)
        }
    }


    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove) {
        when (action) {
            ADD -> repo.addGuildCommand(guildId, command)
            REMOVE -> repo.removeGuildCommand(guildId, command)
        }
    }

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove) {
        when (action) {
            ADD -> repo.addGroupCommand(group, command)
            REMOVE -> repo.removeGroupCommand(group, command)
        }
    }

    suspend fun startupCheck(allFeatureNames: Set<String>): Set<Long> {
        val botOwnerGuild = botConstants.botOwnerGuildId
        val allFeaturesOnMyGuild = repo.getFeaturesForGuild(botOwnerGuild)
        val addedFeatures = allFeatureNames - allFeaturesOnMyGuild
        val removedFeatures = allFeaturesOnMyGuild - allFeatureNames
        val updatedGuilds = mutableSetOf<Long>()
        if (addedFeatures.isNotEmpty()) {
            repo.addFeaturesToGuild(botOwnerGuild, addedFeatures)
            updatedGuilds.add(botOwnerGuild)
        }
        if (removedFeatures.isNotEmpty()) {
            logger.warn { "Features removed from codebase: $removedFeatures. Removing them from all guilds." }
            val affectedGuilds = repo.getGuildsAffectedByCommands(removedFeatures)
            updatedGuilds.addAll(affectedGuilds)
            repo.removeFeaturesGlobally(removedFeatures)
        }
        return updatedGuilds
    }

}
