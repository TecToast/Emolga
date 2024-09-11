package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import mu.KotlinLogging

object SearchReplaysCommand : CommandFeature<SearchReplaysCommand.Args>(
    ::Args,
    CommandSpec(
        "searchreplays",
        "Sucht nach Replays der angegebenen Showdownbenutzernamen",
        Constants.G.ASL,
        Constants.G.FLP
    )
) {
    class Args : Arguments() {
        var user1 by string("User 1", "Der Showdown-Username von jemandem")
        var user2 by string("User 2", "Der Showdown-Username eines potenziellen zweiten Users").nullable()
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val u1 = e.user1
        val u2 = e.user2
        deferReply()
        val body = getBody(u1, u2)
        logger.info(body)
        val jsonstring = if (body.length < 30) {
            reply("Verbindung mit dem Showdown-Server fehlgeschlagen, ich versuche es in 10 Sekunden erneut...")
            delay(10000)
            getBody(u1, u2)
        } else body
        try {
            reply(
                otherJSON.decodeFromString<List<Replay>>(jsonstring).take(15)
                    .joinToString("\n") { "${it.players.joinToString(" vs ")}: https://replay.pokemonshowdown.com/${it.id}" }
                    .ifEmpty { "Es wurde kein Kampf ${u2?.let { "zwischen $u1 und $it" } ?: "von $u1"} hochgeladen!" })
        } catch (ex: Exception) {
            reply("Es konnte keine Verbindung zum Showdown-Server hergestellt werden!")
        }
    }

    private suspend fun getBody(u1: String, u2: String?) = httpClient.get(
        "https://replay.pokemonshowdown.com/search.json?user=${u1.toUsername()}".notNullAppend(u2?.let {
            "&user2=${it.toUsername()}"
        }).also { logger.info(it) }
    ).bodyAsText()


    private val logger = KotlinLogging.logger {}


    @Serializable
    data class Replay(val id: String, val players: List<String>, val uploadtime: Long, val format: String)
}
