package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBData
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBUser
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.repository.DSBConfigRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class DSBHostService(
    private val dsbFlow: DSBFlow,
    private val dsbConfigRepo: DSBConfigRepository,
    private val discordUserService: DiscordUserService,
    @Named("web") private val webJson: Json
) {
    suspend fun getDSBData(host: Long): DSBData? {
        val dsb = dsbConfigRepo.getDSBConfig(host) ?: return null
        val userData = discordUserService.getData(dsb.guild, dsb.users)
        val users = userData.map { (id, data) ->
            DSBUser(
                id = id.toString(),
                name = data.displayName,
                avatar = data.avatarUrl.replace(".gif", ".png")
            )
        }
        return DSBData(users, dsb.categories)
    }

    suspend fun collectSubmissions(host: Long, send: suspend (String) -> Unit): Unit? {
        val dsb = dsbConfigRepo.getDSBConfig(host) ?: return null
        val users = dsb.users.toSet()
        dsbFlow.collect { msg ->
            if (msg.userId.toLongOrNull() !in users) return@collect
            send(webJson.encodeToString(msg))
        }
        return Unit
    }
}