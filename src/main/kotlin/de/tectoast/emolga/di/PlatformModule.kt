package de.tectoast.emolga.di

import de.tectoast.emolga.utils.ratelimiter.CoroutineRateLimiter
import de.tectoast.emolga.utils.ratelimiter.RateLimiter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Module(includes = [JsonModule::class])
class PlatformModule {
    private val defaultCoroutineLogger = KotlinLogging.logger("DefaultCoroutineLogger")
    @Single
    fun defaultScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { ctx, t ->
            val name = ctx[CoroutineName]?.name ?: "Unknown"
            defaultCoroutineLogger.error(t) { "Error in $name" }
        })

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
