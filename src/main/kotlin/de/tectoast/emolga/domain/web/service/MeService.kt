package de.tectoast.emolga.domain.web.service

import de.tectoast.emolga.domain.web.model.DiscordMeData
import org.koin.core.annotation.Single

@Single
class MeService {
    fun getMeData(userId: Long, displayName: String, avatar: String): DiscordMeData {
        return DiscordMeData(
            userId = userId.toString(),
            displayName = displayName,
            avatar = avatar
        )
    }
}