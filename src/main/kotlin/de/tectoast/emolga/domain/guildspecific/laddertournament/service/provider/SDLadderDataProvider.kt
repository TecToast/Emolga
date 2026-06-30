package de.tectoast.emolga.domain.guildspecific.laddertournament.service.provider

import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderUserResponse
import de.tectoast.emolga.utils.toShowdownUserId
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Single
class SDLadderDataProvider(val httpClient: HttpClient) : LadderDataProvider {
    override suspend fun fetchDataForUser(sdName: String): LadderUserResponse {
        delay(Random.nextLong(5000, 10000).milliseconds)
        repeat(5) { _ ->
            val response = httpClient.get("https://pokemonshowdown.com/users/${sdName.toShowdownUserId()}.json")
            if (response.status.isSuccess()) {
                return response.body<LadderUserResponse>()
            }
            delay(Random.nextLong(5000, 15000).milliseconds)
        }
        error("Failed to fetch data for user $sdName")
    }
}
