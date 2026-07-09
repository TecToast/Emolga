package de.tectoast.emolga.domain.cmdmanage.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class CommandManagementRepository(private val db: R2dbcDatabase) {

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


    suspend fun getFeaturesForGuild(gid: Long): Set<String> = suspendTransaction(db) {
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
        }.union(
            GroupCommandsTable.innerJoin(GuildGroupsTable, { this.group }, { this.group })
                .select(GuildGroupsTable.guild).where {
                    (GroupCommandsTable.command inList commands)
                }
        ).map { it[GuildCommandsTable.guild] }.toSet()
    }

    suspend fun removeFeaturesGlobally(features: Set<String>) = suspendTransaction(db) {
        GuildCommandsTable.deleteWhere { command inList features }
        GroupCommandsTable.deleteWhere { command inList features }
    }
}

object GuildGroupsTable : Table("cmd_guild_groups") {
    val guild = long("guild")
    val group = text("group")

    override val primaryKey = PrimaryKey(guild, group)
}

object GuildCommandsTable : Table("cmd_guild_commands") {
    val guild = long("guild")
    val command = text("command")

    override val primaryKey = PrimaryKey(guild, command)
}

object GroupCommandsTable : Table("cmd_group_commands") {
    val group = text("group")
    val command = text("command")

    override val primaryKey = PrimaryKey(group, command)
}