package de.tectoast.emolga.ktor

import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.ignoreDuplicatesMongo
import de.tectoast.emolga.utils.json.YTVideo
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.surroundWith
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.litote.kmongo.exists
import org.litote.kmongo.serialization.TemporalExtendedJsonSerializer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

private val ytClient = HttpClient(CIO) {

}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun setupYTSuscribtions() {
    db.drafts.find(League::replayDataStore exists true).toFlow()
        .flatMapMerge { it.table.asFlow().mapNotNull { u -> db.ytchannel.get(u)?.channelId } }
        .collect { subscribeToYTChannel(it); delay(1000) }
}

private val mac: Mac by lazy {
    val algorithm = "HmacSHA1"
    Mac.getInstance(algorithm).apply {
        init(
            SecretKeySpec(Credentials.tokens.subscriber.secret.toByteArray(), algorithm)
        )
    }
}

fun Route.ytSubscriptions() {
    route("/youtube") {
        get {
            call.respondText(call.request.queryParameters["hub.challenge"]!!)
        }
        post {
            call.respondText(status = HttpStatusCode.Accepted) { "" }
            val receiveText = call.receiveText()
            if (call.request.headers["X-Hub-Signature"]?.substringAfter("=") != mac.doFinal(receiveText.toByteArray())
                    .let {
                        it.joinToString("") { b -> "%02x".format(b) }
                    }
            ) return@post SendFeatures.sendToMe(
                "Invalid Signature! ${call.request.headers["X-Hub-Signature"]} ```$receiveText```".take(2000)
            )
            SendFeatures.sendToMe(receiveText.take(2000 - 6).surroundWith("```"))
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
                if (ignoreDuplicatesMongo {
                        db.ytvideos.insertOne(YTVideo(channelId, videoId, title, Instant.parse(published)))
                    } && "IPL" in title) {
                    val uid = db.ytchannel.get(channelId)!!.user
                    League.executeOnFreshLock({ db.leagueByGuild(Constants.G.VIP, uid)!! }) {
                        val data =
                            replayDataStore!!.getLastEnabledReplayData(uid) ?: return@forEach SendFeatures.sendToMe(
                                "No ReplayData found for $uid in $leaguename"
                            )
                        val ytSave = data.ytVideoSaveData
                        ytSave.vids[battleorder[data.gamedayData.gameday]!![data.gamedayData.battleindex].indexOf(
                            table.indexOf(
                                uid
                            )
                        )] = videoId
                        if (!data.checkIfBothVideosArePresent(this))
                            save("YTSubSave")
                    }
                }
            }
        }
    }
}

object InstantAsDateSerializer : TemporalExtendedJsonSerializer<Instant>() {
    override fun epochMillis(temporal: Instant): Long {
        return temporal.toEpochMilliseconds()
    }

    override fun instantiate(date: Long): Instant {
        return Instant.fromEpochMilliseconds(date)
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
