package de.tectoast.emolga.utils.api

import de.tectoast.emolga.commands.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*

enum class APIManager(val baseUrl: String) {

    TENOR("https://tenor.googleapis.com/v2/");

    operator fun get(path: String) = Endpoint(path)

    inner class Endpoint(val path: String) {
        suspend inline operator fun <reified T> invoke(vararg args: Pair<String, String>): T =
            httpClient.get("$baseUrl$path?${args.joinToString("&") { "${it.first}=${it.second}" }}").body()
    }
}

