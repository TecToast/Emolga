package de.tectoast.emolga.domain.guildspecific.laddertournament.service.provider

import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderUserResponse

interface LadderDataProvider {
    suspend fun fetchDataForUser(sdName: String): LadderUserResponse
}