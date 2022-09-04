package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory
import java.io.IOException

class SearchReplaysCommand :
    Command("searchreplays", "Sucht nach Replays der angegebenen Showdownbenutzernamen", CommandCategory.Showdown) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user1", "User 1", "Der Showdown-Username von jemandem", ArgumentManagerTemplate.Text.any()).add(
                "user2",
                "User 2",
                "Der Showdown-Username eines potenziellen zweiten Users",
                ArgumentManagerTemplate.Text.any(),
                true
            ).setExample("!searchreplays TecToast").build()
        slash(false)
    }

    @Throws(IOException::class)
    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val u1 = args.getText("user1")
        val u2 = args.getNullable<String>("user2")
        e.deferReply()
        val body = getBody(u1, u2)
        logger.info(body)
        val jsonstring = if (body.length < 30) {
            e.hook.sendMessage("Verbindung mit dem Showdown-Server fehlgeschlagen, ich versuche es in 10 Sekunden erneut...")
                .queue()
            delay(10000)
            getBody(u1, u2)
        } else body
        try {
            e.hook.sendMessage(
                JSON.decodeFromString<List<Replay>>(jsonstring).take(15)
                    .joinToString("\n") { "${it.p1} vs ${it.p2}: https://replay.pokemonshowdown.com/${it.id}" }
                    .ifEmpty { "Es wurde kein Kampf ${u2?.let { "zwischen $u1 und $it" } ?: "von $u1"} hochgeladen!" })
                .queue()
        } catch (ex: Exception) {
            e.hook.sendMessage("Es konnte keine Verbindung zum Showdown-Server hergestellt werden!").queue()
        }
    }

    private suspend fun getBody(u1: String, u2: String?) = httpClient.get(
        "https://replay.pokemonshowdown.com/search.json?user=${toUsername(u1)}".notNullAppend(u2?.let {
            "&user2=${toUsername(it)}"
        }).also { logger.info(it) }
    ).bodyAsText()

    companion object {
        private val logger = LoggerFactory.getLogger(SearchReplaysCommand::class.java)
    }

    @Serializable
    data class Replay(val id: String, val p1: String, val p2: String, val uploadtime: Long, val format: String)
}