package de.tectoast.emolga.features.system

import de.tectoast.emolga.domain.cmdmanage.model.AddRemove

interface CommandRegistryService {
    suspend fun updateCommandsForGuild(gid: Long)

    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove)

    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove)

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove)
}