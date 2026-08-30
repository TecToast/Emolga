package de.tectoast.emolga.domain.ytgeneric.service

import de.tectoast.emolga.utils.BotConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.core.annotation.Single

@Single
class YouTubeSubscriptionService(
    private val httpClient: HttpClient,
    private val subscriberConfig: BotConfig.Subscriber
) {

    suspend fun subscribeToChannel(channelId: String) {
        val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId"
        httpClient.post("https://pubsubhubbub.appspot.com/subscribe") {
            setBody(FormDataContent(Parameters.build {
                append("hub.callback", subscriberConfig.callback)
                append("hub.mode", "subscribe")
                append("hub.topic", topic)
                append("hub.verify", "async")
                append("hub.secret", subscriberConfig.secret)
            }))
        }.bodyAsText()
    }
}