package de.tectoast.emolga.utils.json

import kotlinx.serialization.Serializable

@Serializable
data class Tokens(
    val google: Google,
    val discord: String,
    val discordflegmon: String? = null,
    val discordraikou: String? = null,
    val subscriber: Subscriber,
    val database: Database,
    val mongoDB: String,
    val oauth2: Oauth2,
    val artworkPath: String
) {
    @Serializable
    data class Google(
        val refreshtoken: String,
        val clientid: String,
        val clientsecret: String,
        val logoParentId: String,
    )

    @Serializable
    data class Subscriber(
        val secret: String,
        val callback: String,
    )

    @Serializable
    data class Database(
        val username: String,
        val password: String,
        val host: String
    )

    @Serializable
    data class Oauth2(
        val clientsecret: String
    )
}

