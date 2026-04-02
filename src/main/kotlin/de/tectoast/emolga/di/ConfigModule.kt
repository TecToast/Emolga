package de.tectoast.emolga.di

import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.myJSON
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

@Module
class ConfigModule {
    private val tokens: Tokens by lazy {
        myJSON.decodeFromString(
            File(System.getenv("CONFIG_FILE") ?: "config.json").readText()
        )
    }

    @Single
    fun google(): Tokens.Google = tokens.google

    @Single
    @Named("discordToken")
    fun discord(): String = tokens.discord

    @Single
    @Named("discordFlegmonToken")
    fun discordFlegmon(): String? = tokens.discordflegmon

    @Single
    fun subscriber(): Tokens.Subscriber = tokens.subscriber

    @Single
    fun database(): Tokens.Database = tokens.database

    @Single
    @Named("mongoDB")
    fun mongoDB(): String = tokens.mongoDB

    @Single
    fun oauth2(): Tokens.Oauth2 = tokens.oauth2

    @Single
    fun staticCloud(): Tokens.StaticCloud = tokens.staticCloud
}
