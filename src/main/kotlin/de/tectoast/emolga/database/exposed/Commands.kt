package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.flo.AddRemove
import de.tectoast.emolga.utils.Constants
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


object GuildGroupsTable : Table("cmd_guild_groups") {
    val guild = long("guild")
    val group = varchar("group", 50)

    override val primaryKey = PrimaryKey(guild, group)
}

object GuildCommandsTable : Table("cmd_guild_commands") {
    val guild = long("guild")
    val command = varchar("command", 50)

    override val primaryKey = PrimaryKey(guild, command)
}

object GroupCommandsTable : Table("cmd_group_commands") {
    val group = varchar("group", 50)
    val command = varchar("command", 50)

    override val primaryKey = PrimaryKey(group, command)
}

@Single
class CommandManagementRepository(val db: R2dbcDatabase) {

    suspend fun addGuildGroup(guildId: Long, group: String) = suspendTransaction(db) {
        GuildGroupsTable.insertIgnore {
            it[GuildGroupsTable.guild] = guildId
            it[GuildGroupsTable.group] = group
        }
    }

    suspend fun removeGuildGroup(guildId: Long, group: String) = suspendTransaction(db) {
        GuildGroupsTable.deleteWhere {
            (GuildGroupsTable.guild eq guildId) and (GuildGroupsTable.group eq group)
        }
    }

    suspend fun addGuildCommand(guildId: Long, command: String) = suspendTransaction(db) {
        GuildCommandsTable.insertIgnore {
            it[GuildCommandsTable.guild] = guildId
            it[GuildCommandsTable.command] = command
        }
    }

    suspend fun removeGuildCommand(guildId: Long, command: String) = suspendTransaction(db) {
        GuildCommandsTable.deleteWhere {
            (GuildCommandsTable.guild eq guildId) and (GuildCommandsTable.command eq command)
        }
    }

    suspend fun addGroupCommand(group: String, command: String) = suspendTransaction(db) {
        GroupCommandsTable.insertIgnore {
            it[GroupCommandsTable.group] = group
            it[GroupCommandsTable.command] = command
        }
    }

    suspend fun removeGroupCommand(group: String, command: String) = suspendTransaction(db) {
        GroupCommandsTable.deleteWhere {
            (GroupCommandsTable.group eq group) and (GroupCommandsTable.command eq command)
        }
    }

    suspend fun getGuildsForGroup(group: String) = suspendTransaction(db) {
        GuildGroupsTable.select(GuildGroupsTable.guild).where { GuildGroupsTable.group eq group }
            .map { it[GuildGroupsTable.guild] }.toSet()
    }


    suspend fun getFeaturesForGuild(gid: Long): Set<String> = dbTransaction {
        GuildCommandsTable.select(GuildCommandsTable.command).where { GuildCommandsTable.guild eq gid }.union(
            GroupCommandsTable.select(
                GroupCommandsTable.command
            ).where {
                GroupCommandsTable.group inSubQuery GuildGroupsTable.select(GuildGroupsTable.group)
                    .where { GuildGroupsTable.guild eq gid }
            }).map { it[GuildCommandsTable.command] }.toSet()
    }


    suspend fun getAllGuildTargets() = suspendTransaction(db) {
        GuildGroupsTable.select(GuildGroupsTable.guild).union(GuildCommandsTable.select(GuildCommandsTable.guild))
            .map { it[GuildGroupsTable.guild] }.toSet()
    }

    suspend fun getGroups() = suspendTransaction(db) {
        GuildGroupsTable.select(GuildGroupsTable.group).withDistinct(true).orderBy(GuildGroupsTable.group)
            .map { it[GuildGroupsTable.group] }.toList()
    }

    suspend fun addFeaturesToGuild(guildId: Long, features: Iterable<String>) = suspendTransaction(db) {
        GuildCommandsTable.batchInsert(features) { feature ->
            this[GuildCommandsTable.guild] = guildId
            this[GuildCommandsTable.command] = feature
        }
    }

    suspend fun getGuildsAffectedByCommands(commands: Iterable<String>): Set<Long> = suspendTransaction(db) {
        GuildCommandsTable.select(GuildCommandsTable.guild).where {
            (GuildCommandsTable.command inList commands)
        }.union(GroupCommandsTable.select(GroupCommandsTable.group).where {
            (GroupCommandsTable.command inList commands)
        }).map { it[GuildCommandsTable.guild] }.toSet()
    }

    suspend fun removeFeaturesGlobally(features: Set<String>) = suspendTransaction(db) {
        GuildCommandsTable.deleteWhere { command inList features }
        GroupCommandsTable.deleteWhere { command inList features }
    }
}

@Single
class CommandManagementService(
    val repo: CommandManagementRepository
) {

    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove) {
        if (action.add()) {
            repo.addGuildGroup(guildId, group)
        } else {
            repo.removeGuildGroup(guildId, group)
        }
    }


    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove) {
        if (action.add()) {
            repo.addGuildCommand(guildId, command)
        } else {
            repo.removeGuildCommand(guildId, command)
        }
    }

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove) {
        if (action.add()) {
            repo.addGroupCommand(group, command)
        } else {
            repo.removeGroupCommand(group, command)
        }
    }

    suspend fun startupCheck(allFeatureNames: Set<String>): Set<Long> {
        val allFeaturesOnMyGuild = repo.getFeaturesForGuild(Constants.G.MY)
        val addedFeatures = allFeatureNames - allFeaturesOnMyGuild
        val removedFeatures = allFeaturesOnMyGuild - allFeatureNames
        val updatedGuilds = mutableSetOf<Long>()
        if (addedFeatures.isNotEmpty()) {
            repo.addFeaturesToGuild(Constants.G.MY, addedFeatures)
            updatedGuilds.add(Constants.G.MY)
        }
        if (removedFeatures.isNotEmpty()) {
            val affectedGuilds = repo.getGuildsAffectedByCommands(removedFeatures)
            updatedGuilds.addAll(affectedGuilds)
            repo.removeFeaturesGlobally(removedFeatures)
        }
        return updatedGuilds
    }

}
