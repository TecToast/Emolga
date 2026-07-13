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

    @Single
    fun botConfig(): BotConfig = Json.decodeFromString(
        File(System.getenv("CONFIG_FILE") ?: "config.json").readText()
    )

    @Single
    fun google(botConfig: BotConfig): BotConfig.Google = botConfig.google

    @Single
    @Named("discordToken")
    fun discord(botConfig: BotConfig): String = botConfig.discord

    @Single
    @Named("discordFlegmonToken")
    fun discordFlegmon(botConfig: BotConfig): OptionalString =
        botConfig.discordflegmon?.let { OptionalString.Present(it) } ?: OptionalString.Absent

    @Single
    fun subscriber(botConfig: BotConfig): BotConfig.Subscriber = botConfig.subscriber

    @Single
    fun database(botConfig: BotConfig): BotConfig.Database = botConfig.database

    @Single
    fun oauth2(botConfig: BotConfig): BotConfig.Oauth2 = botConfig.oauth2

    @Single
    fun staticCloud(botConfig: BotConfig): BotConfig.StaticCloud = botConfig.staticCloud

    @Single
    fun botConstants(botConfig: BotConfig): BotConstants = botConfig.botConstants
}
