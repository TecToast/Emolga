package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.utils.BotConfig
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.koin.core.annotation.Single
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


@Single
class YouTubePushProcessingService(
    private val subscriberConfig: BotConfig.Subscriber,
    private val youTubeNotificationService: YouTubeNotificationService,
    private val ytLeagueService: YouTubeLeagueService,
    dispatcher: CoroutineDispatcher
) {
    private val scope = createCoroutineScope("YouTubePushNotificationService", dispatcher)
    private val logger = KotlinLogging.logger {}
    private val mac: Mac by lazy {
        Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(subscriberConfig.secret.toByteArray(), "HmacSHA1"))
        }
    }

    fun handleIncoming(body: String, xHubSignature: String) {
        if (!validateSignature(body, xHubSignature)) return
        Jsoup.parse(body).select("entry").forEach {
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
                if (upd - pub > 1.hours) {
                    logger.info("Published: {} Updated: {} Difference: {}", pub, upd, upd - pub)
                    return@forEach
                }
            } catch (e: Exception) {
                logger.error("Error parsing date in YT", e)
            }
            scope.launch {
                youTubeNotificationService.handleIncoming(channelId, videoId)
            }
            scope.launch {
                ytLeagueService.handleIncoming(channelId, videoId, title)
            }
        }
    }

    private fun validateSignature(body: String, xHubSignature: String): Boolean {
        val correctSignatureBytes = mac.doFinal(body.toByteArray())
        val hexSignature = correctSignatureBytes.joinToString("") { b -> "%02x".format(b) }
        return xHubSignature.substringAfter("=") == hexSignature
    }
}