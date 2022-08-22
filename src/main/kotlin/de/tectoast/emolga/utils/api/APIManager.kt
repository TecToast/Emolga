package de.tectoast.emolga.utils.api

import de.tectoast.jsolf.JSONObject
import de.tectoast.jsolf.JSONTokener
import java.net.URL

enum class APIManager(val baseUrl: String) {

    TENOR("https://tenor.googleapis.com/v2/");

    operator fun get(path: String) = Endpoint(path)

    inner class Endpoint(private val path: String) {
        operator fun invoke(vararg args: Pair<String, String>) =
            JSONObject(JSONTokener(URL("$baseUrl$path?${args.joinToString("&") { "${it.first}=${it.second}" }}").openStream()))
    }
}

