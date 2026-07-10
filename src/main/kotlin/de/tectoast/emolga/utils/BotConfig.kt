package de.tectoast.emolga.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val google: Google,
    val discord: String,
    val discordflegmon: String? = null,
    val subscriber: Subscriber,
    val database: Database,
    val oauth2: Oauth2,
    val staticCloud: StaticCloud,
    val botConstants: BotConstants = BotConstants()
) {
    @Serializable
    data class Google(
        val refreshtoken: String,
        val clientid: String,
        val clientsecret: String,
    )

    @Serializable
    data class Subscriber(
        val secret: String,
        val callback: String,
    )

    @Serializable
    sealed interface Database {
        val username: String
        val password: String
        val database: String
        val statisticDatabase: String

        @Serializable
        @SerialName("Network")
        data class Network(
            override val username: String,
            override val password: String,
            override val database: String = "emolga",
            override val statisticDatabase: String = "emolga_stats",
            val host: String,
            val port: Int,
        ) : Database

        @Serializable
        @SerialName("Socket")
        data class Socket(
            override val username: String,
            override val password: String,
            override val database: String = "emolga",
            override val statisticDatabase: String = "emolga_stats",
            val path: String,
        ) : Database
    }


    @Serializable
    data class Oauth2(
        val clientid: String,
        val clientsecret: String,
        val callbackUrl: String,
        val nonceKey: String,
        val devMode: Boolean = false,
    )

    @Serializable
    data class StaticCloud(val token: String, val baseUrl: String, val hashLength: Int = 24)


}

@Serializable
data class BotConstants(
    val botOwnerId: Long = 0,
    val botOwnerTag: String = "",
    val botOwnerGuildId: Long = 0,
    val calendarChannelId: Long = 0,
    val calendarMessageId: Long = 0,
    val dmRelayChannelId: Long = 0,
    val dbMigrationChannelId: Long = 0,
    val flegmonGuildId: Long = 0,
    val flegmonBirthdayChannelId: Long = 0,
    val flegmonAcceptedRulesRoleId: Long = 0,
    val flegmonRoleChannelId: Long = 0,
    val gpcAdminRoleId: Long = 0,
    val sdNamesChannelId: Long = 0,
    val always17User: Long = 0,
    val webBaseUrl: String = "https://example.com",
    val logoBaseUrl: String = "https://example.com/images",
)