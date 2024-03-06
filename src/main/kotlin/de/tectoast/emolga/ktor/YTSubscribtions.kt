package de.tectoast.emolga.ktor

import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.ignoreDuplicatesMongo
import de.tectoast.emolga.utils.json.YTVideo
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.mapNotNull
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.litote.kmongo.exists
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

private val ytClient = HttpClient(CIO) {

}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun setupYTSuscribtions() {
    db.drafts.find(League::replayDataStore exists true).toFlow()
        .flatMapMerge { it.table.asFlow().mapNotNull { u -> db.ytchannel.get(u)?.channelId } }
        .collect { subscribeToYTChannel(it) }
}

private val mac: Mac by lazy {
    val algorithm = "HmacSHA1"
    Mac.getInstance(algorithm).apply {
        init(
            SecretKeySpec(Credentials.tokens.subscriber.secret.toByteArray(), algorithm)
        )
    }
}

fun Route.ytSubscribtions() {
    route("/youtube") {
        get {
            call.respondText(call.request.queryParameters["hub.challenge"]!!)
        }
        post {
            call.respondText(status = HttpStatusCode.Accepted) { "" }
            val receiveText = call.receiveText()
            if (call.request.headers["X-Hub-Signature"] != mac.doFinal(receiveText.toByteArray()).let {
                    it.joinToString("") { b -> "%02x".format(b) }
                }) return@post SendFeatures.sendToMe(
                "Invalid Signature! ${call.request.headers.entries()} $receiveText".take(2000)
            )
            Jsoup.parse(receiveText).select("entry").forEach {
                val title = it.select("title").text()
                val link = it.select("link").attr("href")
                val channelId = it.select("author").select("uri").text().substringAfterLast("/")
                val published = it.select("published").text()
                val updated = it.select("updated").text()
                val videoId = it.select("yt\\:videoId").text()
                logger.info("New video by $channelId: $title")
                logger.info("Link: $link")
                logger.info("Published: $published")
                logger.info("Updated: $updated")
                ignoreDuplicatesMongo {
                    db.ytvideos.insertOne(YTVideo(channelId, videoId, title, published, updated))
                }
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
                append("hub.secret", Credentials.tokens.subscriber.secret)
            }))
        }.bodyAsText()
    )
}
