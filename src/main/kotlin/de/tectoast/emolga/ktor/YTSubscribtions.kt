package de.tectoast.emolga.ktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.jsoup.Jsoup

private val logger = KotlinLogging.logger {}


private val ytClient = HttpClient(CIO) {

}

fun Route.ytSubscribtions() {
    route("/youtube") {
        get {
            logger.info(call.request.queryString())
            logger.info(call.request.queryParameters["hub.challenge"])
            call.respondText(call.request.queryParameters["hub.challenge"]!!)
        }
        post {
            call.respondText(status = HttpStatusCode.Accepted) { "" }
            Jsoup.parse(call.receiveText()).select("entry").forEach {
                val title = it.select("title").text()
                val link = it.select("link").attr("href")
                //val id = it.select("yt:videoId").text()
                val author = it.select("author").select("name").text()
                val published = it.select("published").text()
                val updated = it.select("updated").text()
                //val thumbnail = it.select("media\\:thumbnail").attr("url")
                logger.info("New video by $author: $title")
                logger.info("Link: $link")
                //logger.info("ID: $id")
                logger.info("Published: $published")
                logger.info("Updated: $updated")
                //logger.info("Thumbnail: $thumbnail")
            }
        }
    }
}

suspend fun subscribeToYTChannel(channelID: String) {
    logger.info(
        ytClient.post("https://pubsubhubbub.appspot.com/subscribe") {
            setBody(FormDataContent(Parameters.build {
                append("hub.callback", "https://emolga.tectoast.de/api/youtube")
                append("hub.mode", "subscribe")
                append("hub.topic", "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelID")
                append("hub.verify", "async")
            }))
        }.bodyAsText()
    )
}
