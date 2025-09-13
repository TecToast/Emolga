package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.flo.AddRemove
import de.tectoast.emolga.utils.Constants
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*

object GuildGroupsDB : Table("cmd_guild_groups") {
    val GUILD = long("guild")
    val GROUP = varchar("group", 50)

    override val primaryKey = PrimaryKey(GUILD, GROUP)
}

object GuildCommandsDB : Table("cmd_guild_commands") {
    val GUILD = long("guild")
    val COMMAND = varchar("command", 50)

    override val primaryKey = PrimaryKey(GUILD, COMMAND)
}

object GroupCommandsDB : Table("cmd_group_commands") {
    val GROUP = varchar("group", 50)
    val COMMAND = varchar("command", 50)

    override val primaryKey = PrimaryKey(GROUP, COMMAND)
}

object CmdManager {

    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove) {
        dbTransaction {
            if (action.add()) {
                GuildGroupsDB.insertIgnore {
                    it[GUILD] = guildId
                    it[GuildGroupsDB.GROUP] = group
                }
            } else {
                GuildGroupsDB.deleteWhere { (GUILD eq guildId) and (GuildGroupsDB.GROUP eq group) }
            }
        }
        EmolgaMain.featureManager().updateCommandsForGuild(guildId)
    }


    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove) {
        dbTransaction {
            if (action.add()) {
                GuildCommandsDB.insertIgnore {
                    it[GUILD] = guildId
                    it[COMMAND] = command
                }
            } else {
                GuildCommandsDB.deleteWhere { (GUILD eq guildId) and (COMMAND eq command) }
            }
        }
        EmolgaMain.featureManager().updateCommandsForGuild(guildId)
    }

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove) {
        dbTransaction {
            if (action.add()) {
                GroupCommandsDB.insertIgnore {
                    it[GroupCommandsDB.GROUP] = group
                    it[GroupCommandsDB.COMMAND] = command
                }
            } else {
                GroupCommandsDB.deleteWhere { (GroupCommandsDB.GROUP eq group) and (GroupCommandsDB.COMMAND eq command) }
            }
        }
        updateAllGuildsInGroup(group)
    }

    private suspend fun updateAllGuildsInGroup(group: String) {
        dbTransaction {
            GuildGroupsDB.select(GuildGroupsDB.GUILD).where { GuildGroupsDB.GROUP eq group }.forEach {
                EmolgaMain.featureManager().updateCommandsForGuild(it[GuildGroupsDB.GUILD])
                delay(2000) // avoid rate limits
            }
        }
    }

    suspend fun getFeaturesForGuild(gid: Long): Set<String> = dbTransaction {
        GuildCommandsDB.select(GuildCommandsDB.COMMAND).where { GuildCommandsDB.GUILD eq gid }.union(
            GroupCommandsDB.select(
                GroupCommandsDB.COMMAND
            ).where {
                GroupCommandsDB.GROUP inSubQuery GuildGroupsDB.select(GuildGroupsDB.GROUP)
                    .where { GuildGroupsDB.GUILD eq gid }
            }).map { it[GuildCommandsDB.COMMAND] }.toSet()
    }

    suspend fun startupCheck() {
        val allFeatures = EmolgaMain.featureManager().registeredFeatureList
        val allFeatureNames = allFeatures.map { it.spec.name }.toSet()

        val allFeaturesOnMyGuild = getFeaturesForGuild(Constants.G.MY)
        val addedFeatures = allFeatureNames - allFeaturesOnMyGuild
        val removedFeatures = allFeaturesOnMyGuild - allFeatureNames
        val updatedGuilds = mutableSetOf<Long>()
        if (addedFeatures.isNotEmpty()) {
            dbTransaction {
                GuildCommandsDB.batchInsert(addedFeatures) {
                    this[GuildCommandsDB.GUILD] = Constants.G.MY
                    this[GuildCommandsDB.COMMAND] = it
                }
                updatedGuilds.add(Constants.G.MY)
            }
        }
        if (removedFeatures.isNotEmpty()) {
            dbTransaction {
                val affectedGuilds = (GuildCommandsDB.select(GuildCommandsDB.GUILD).where {
                    (GuildCommandsDB.COMMAND inList removedFeatures)
                } union GroupCommandsDB.select(GroupCommandsDB.GROUP).where {
                    (GroupCommandsDB.COMMAND inList removedFeatures)
                }).map { it[GuildCommandsDB.GUILD] }.toSet()

                updatedGuilds.addAll(affectedGuilds)
                GuildCommandsDB.deleteWhere {
                    (GuildCommandsDB.COMMAND inList removedFeatures)
                }
                GroupCommandsDB.deleteWhere {
                    (GroupCommandsDB.COMMAND inList removedFeatures)
                }
            }
        }
        for (gid in updatedGuilds) {
            EmolgaMain.featureManager().updateCommandsForGuild(gid)
            delay(2000) // avoid rate limits
        }
    }
}