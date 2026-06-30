package de.tectoast.emolga.di

import de.tectoast.emolga.utils.BotConfig
import de.tectoast.emolga.utils.BotConstants
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

@Module
class ConfigModule {
    private val botConfig: BotConfig by lazy {
        Json.decodeFromString(
            File(System.getenv("CONFIG_FILE") ?: "config.json").readText()
        )
    }

    @Single
    fun google(): BotConfig.Google = botConfig.google

    @Single
    @Named("discordToken")
    fun discord(): String = botConfig.discord

    @Single
    @Named("discordFlegmonToken")
    fun discordFlegmon(): OptionalString =
        botConfig.discordflegmon?.let { OptionalString.Present(it) } ?: OptionalString.Absent

    @Single
    fun subscriber(): BotConfig.Subscriber = botConfig.subscriber

    @Single
    fun database(): BotConfig.Database = botConfig.database

    @Single
    fun oauth2(): BotConfig.Oauth2 = botConfig.oauth2

    @Single
    fun staticCloud(): BotConfig.StaticCloud = botConfig.staticCloud

    @Single
    fun botConstants(): BotConstants = botConfig.botConstants
}
