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

@Single
class GuildGroupsDB : Table("cmd_guild_groups") {
    val guild = long("guild")
    val group = varchar("group", 50)

    override val primaryKey = PrimaryKey(guild, group)
}

@Single
class GuildCommandsDB : Table("cmd_guild_commands") {
    val guild = long("guild")
    val command = varchar("command", 50)

    override val primaryKey = PrimaryKey(guild, command)
}

@Single
class GroupCommandsDB : Table("cmd_group_commands") {
    val group = varchar("group", 50)
    val command = varchar("command", 50)

    override val primaryKey = PrimaryKey(group, command)
}

interface CommandManager {
    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove)

    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove)

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove)

    suspend fun getFeaturesForGuild(gid: Long): Set<String>

    suspend fun getAllGuildTargets(): Set<Long>

    suspend fun getGroups(): List<String>

    suspend fun getGuildsForGroup(group: String): Set<Long>

    suspend fun startupCheck(allFeatureNames: Set<String>): Set<Long>
}

@Single
class PostgresCommandManager(
    val db: R2dbcDatabase,
    val guildGroups: GuildGroupsDB,
    val guildCommands: GuildCommandsDB,
    val groupCommands: GroupCommandsDB
) : CommandManager {

    override suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove) {
        suspendTransaction(db) {
            if (action.add()) {
                guildGroups.insertIgnore {
                    it[guildGroups.guild] = guildId
                    it[guildGroups.group] = group
                }
            } else {
                guildGroups.deleteWhere { (guildGroups.guild eq guildId) and (guildGroups.group eq group) }
            }
        }
    }


    override suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove) {
        suspendTransaction(db) {
            if (action.add()) {
                guildCommands.insertIgnore {
                    it[guildCommands.guild] = guildId
                    it[guildCommands.command] = command
                }
            } else {
                guildCommands.deleteWhere { (guildCommands.guild eq guildId) and (guildCommands.command eq command) }
            }
        }
    }

    override suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove) {
        suspendTransaction(db) {
            if (action.add()) {
                groupCommands.insertIgnore {
                    it[groupCommands.group] = group
                    it[groupCommands.command] = command
                }
            } else {
                groupCommands.deleteWhere { (groupCommands.group eq group) and (groupCommands.command eq command) }
            }
        }
    }

    override suspend fun getGuildsForGroup(group: String) = suspendTransaction(db) {
        guildGroups.select(guildGroups.guild).where { guildGroups.group eq group }.map { it[guildGroups.guild] }.toSet()
    }


    override suspend fun getFeaturesForGuild(gid: Long): Set<String> = dbTransaction {
        guildCommands.select(guildCommands.command).where { guildCommands.guild eq gid }.union(
            groupCommands.select(
                groupCommands.command
            ).where {
                groupCommands.group inSubQuery guildGroups.select(guildGroups.group)
                    .where { guildGroups.guild eq gid }
            }).map { it[guildCommands.command] }.toSet()
    }

    override suspend fun startupCheck(allFeatureNames: Set<String>): Set<Long> {
        val allFeaturesOnMyGuild = getFeaturesForGuild(Constants.G.MY)
        val addedFeatures = allFeatureNames - allFeaturesOnMyGuild
        val removedFeatures = allFeaturesOnMyGuild - allFeatureNames
        val updatedGuilds = mutableSetOf<Long>()
        if (addedFeatures.isNotEmpty()) {
            suspendTransaction(db) {
                guildCommands.batchInsert(addedFeatures) {
                    this[guildCommands.guild] = Constants.G.MY
                    this[guildCommands.command] = it
                }
                updatedGuilds.add(Constants.G.MY)
            }
        }
        if (removedFeatures.isNotEmpty()) {
            suspendTransaction(db) {
                val affectedGuilds = guildCommands.select(guildCommands.guild).where {
                    (guildCommands.command inList removedFeatures)
                }.union(groupCommands.select(groupCommands.group).where {
                    (groupCommands.command inList removedFeatures)
                }).map { it[guildCommands.guild] }.toSet()

                updatedGuilds.addAll(affectedGuilds)
                guildCommands.deleteWhere {
                    (guildCommands.command inList removedFeatures)
                }
                groupCommands.deleteWhere {
                    (groupCommands.command inList removedFeatures)
                }
            }
        }
        return updatedGuilds
    }

    override suspend fun getAllGuildTargets() = suspendTransaction(db) {
        guildGroups.select(guildGroups.guild).union(guildCommands.select(guildCommands.guild))
            .map { it[guildGroups.guild] }.toSet()
    }

    override suspend fun getGroups() = suspendTransaction(db) {
        guildGroups.select(guildGroups.group).withDistinct(true).orderBy(guildGroups.group)
            .map { it[guildGroups.group] }.toList()
    }
}
