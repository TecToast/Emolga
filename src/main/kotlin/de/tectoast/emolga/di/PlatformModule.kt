package de.tectoast.emolga.di

import de.tectoast.emolga.utils.ratelimiter.CoroutineRateLimiter
import de.tectoast.emolga.utils.ratelimiter.RateLimiter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Module(includes = [JsonModule::class])
class PlatformModule {
    @Single
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun defaultClock(): Clock = Clock.System

    @Single
    fun httpClient(@Named("web") webJSON: Json) = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(webJSON)
        }
    }

    @Single
    @Named("GoogleRateLimiter")
    fun ratelimiter(clock: Clock): RateLimiter = CoroutineRateLimiter(capacity = 5, tokensPerSecond = 0.8, clock)
}
