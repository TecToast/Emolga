package de.tectoast.emolga.utils.json

import kotlinx.serialization.Serializable

@Serializable
data class Tokens(
    val google: Google,
    val discord: String,
    val discordflegmon: String,
    val subscriber: Subscriber,
    val tradedoc: String,
    val database: Database,
    val website: Website,
    val oauth2: Oauth2,
    val tenor: String
) {
    @Serializable
    data class Google(
        val refreshtoken: String,
        val clientid: String,
        val clientsecret: String
    )

    @Serializable
    data class Subscriber(
        val host: String,
        val port: Int
    )

    @Serializable
    data class Database(
        val username: String,
        val password: String
    )

    @Serializable
    data class Website(
        val path: String,
        val password: String,
        val keyalias: String
    )

    @Serializable
    data class Oauth2(
        val clientsecret: String
    )
}

