package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service

import de.tectoast.emolga.domain.eventbus.EventBus
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBData
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBMessage
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBUser
import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.repository.DSBConfigRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class DSBHostService(
    private val dsbConfigRepo: DSBConfigRepository,
    private val discordUserService: DiscordUserService,
    private val eventBus: EventBus,
    @Named("web") private val webJson: Json,
    baseScope: CoroutineScope
) {
    private val scope = baseScope + CoroutineName("DSBHostService")
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
        eventBus.collectSuspending<DSBMessage> { msg ->
            if (msg.userId.toLongOrNull() !in users) return@collectSuspending
            send(webJson.encodeToString(msg))
        }
        return Unit
    }
}