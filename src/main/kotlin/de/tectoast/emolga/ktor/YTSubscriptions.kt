@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.credentials.Credentials
import de.tectoast.emolga.database.exposed.YTChannelsDB
import de.tectoast.emolga.database.exposed.YTNotificationsDB
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.YouTubeConfig
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.defaultScope
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.generics.getChannel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jsoup.Jsoup
import org.litote.kmongo.div
import org.litote.kmongo.exists
import org.litote.kmongo.serialization.TemporalExtendedJsonSerializer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}
private val duplicateVideoCache = mutableSetOf<String>()
private val recentlySubscribed = mutableSetOf<String>()
private val ytClient = HttpClient(CIO)
private val ytScope = createCoroutineScope("YouTubeSubscriptions")


@OptIn(ExperimentalCoroutinesApi::class)
fun setupYTSuscribtions() {
    defaultScope.launch {
        val allChannels = YTNotificationsDB.getAllYTChannels()
        val fromLeagueUsers =
            db.league.find(League::config / LeagueConfig::youtube / YouTubeConfig::sendChannel exists true).toFlow()
                .flatMapConcat { it.table.asFlow() }.toSet()
        YTChannelsDB.addAllChannelIdsToSet(allChannels, fromLeagueUsers)
        logger.info("Subscribing to ${allChannels.size} channels...")
        allChannels.forEach {
            subscribeToYTChannel(it)
            delay(1000)
        }
        logger.info("Done subscribing to ${allChannels.size} channels!")
    }

}

private val mac: Mac by lazy {
    val algorithm = "HmacSHA1"
    Mac.getInstance(algorithm).apply {
        init(
            SecretKeySpec(Credentials.tokens.subscriber.secret.toByteArray(), algorithm)
        )
    }
}

private suspend inline fun RoutingCall.verifyYT(param: String, check: (String) -> Boolean = { true }): String? {
    val value = request.queryParameters[param]
    if (value == null || !check(value)) {
        logger.warn("Invalid YT verify $param: $value")
        respond(HttpStatusCode.BadRequest)
        return null
    }
    return value
}

fun Route.ytSubscriptions() {
    route("/youtube") {
        get {
            call.verifyYT("hub.mode") { it == "subscribe" } ?: return@get
            val topic = call.verifyYT("hub.topic") { it in recentlySubscribed }
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val challenge = call.verifyYT("hub.challenge") ?: return@get
            logger.debug { "Verified topic: $topic" }
            call.respondText(challenge)
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
//            SendFeatures.sendToMe(receiveText.take(2000 - 6).surroundWith("```"))
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
                try {
                    val pub = Instant.parse(published)
                    val upd = Instant.parse(updated)
                    if (upd - pub > 1.minutes) {
                        return@forEach
                    }
                } catch (e: Exception) {
                    logger.error("Error parsing date in YT", e)
                }
                if (duplicateVideoCache.add(videoId)) {
                    ytScope.launch {
                        YTNotificationsDB.getDCChannels(channelId).forEach { (mc, dm) ->
                            val channel =
                                if (dm) jda.openPrivateChannelById(mc).await()
                                else jda.getChannel<MessageChannel>(mc)
                            channel?.sendMessage("https://youtu.be/$videoId")?.queue()
                        }
                    }
                    ytScope.launch {
                        db.config.only().ytLeagues.forEach { (short, gid) ->
                            if (title.contains(short, ignoreCase = true)) {
                                handleVideo(channelId, videoId, gid)
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun handleVideo(channelId: String, videoId: String, gid: Long) {
    logger.info("Handling video $videoId for channel $channelId in guild $gid")
    val uids = YTChannelsDB.getUsersByChannelId(channelId)
    var successful = false
    for (uid in uids) {
        logger.info("Uid found: $uid")
        val leagues = db.leaguesByGuild(gid, uid)
        for (league in leagues.map { it.leaguename }) {
            logger.info("Checking league: $league")
            League.executeOnFreshLock(league) {
                logger.info("League found: $leaguename")
                val idx = this(uid)
                val data = RepeatTask.getTask(leaguename, RepeatTaskType.RegisterInDoc)?.findGamedayOfDay()
                    ?.let { persistentData.replayDataStore.data[it]?.values?.firstOrNull { data -> idx in data.uindices } }
                    ?: return@executeOnFreshLock
                val ytSave = data.ytVideoSaveData
                if (!ytSave.enabled) {
                    logger.info("YT Save not enabled for $uid in $leaguename")
                    return@executeOnFreshLock
                }
                ytSave.vids[battleorder[data.gamedayData.gameday]!![data.gamedayData.battleindex].indexOf(
                    table.indexOf(
                        uid
                    )
                )] = videoId
                logger.info("Saving video $videoId for $uid in $leaguename")
                data.checkIfBothVideosArePresent(this)
                save("YTSubSave")
                successful = true
            }
            if (successful) return
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
    val config = Credentials.tokens.subscriber
    val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelID"
    recentlySubscribed += topic
    ytClient.post("https://pubsubhubbub.appspot.com/subscribe") {
        setBody(FormDataContent(Parameters.build {
            append("hub.callback", config.callback)
            append("hub.mode", "subscribe")
            append("hub.topic", topic)
            append("hub.verify", "async")
            append("hub.secret", config.secret)
        }))
    }.bodyAsText()

}
