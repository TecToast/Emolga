package de.tectoast.emolga.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.messages.Embed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.dv8tion.jda.api.entities.MessageEmbed
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

class DiscordAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    lateinit var url: String

    private var timeout: String = "10000"
    private var level: String = "warn"

    lateinit var scope: CoroutineScope
    lateinit var client: HttpClient
    lateinit var channel: Channel<MessageEmbed>
    private val throwableConverter = ThrowableProxyConverter().apply {
        start()
    }

    private var timeoutMillis by Delegates.notNull<Long>()


    private fun String.inCodeTicks() = "```$this```"

    override fun append(eventObject: ILoggingEvent) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            channel.send(Embed {
                title = eventObject.loggerName.substringAfterLast(".")
                color = when (eventObject.level) {
                    Level.ERROR -> 0xFF0000
                    Level.WARN -> 0xFFA500
                    else -> Constants.EMBED_COLOR
                }
                field {
                    name = "Message"
                    value = eventObject.formattedMessage.take(1024 - 6).inCodeTicks()
                    inline = false
                }
                eventObject.throwableProxy?.let {
                    field {
                        name = "Exception"
                        value = throwableConverter.convert(eventObject).take(1024 - 6).inCodeTicks()
                        inline = false
                    }
                }
            })
        }
    }

    override fun start() {
        if (!::url.isInitialized) {
            addError("No url set for the appender named [$name]")
            return
        }
        addFilter(ThresholdFilter().apply {
            setLevel(level)
            start()
        })
        addFilter(UnknownInteractionFilter)
        scope = CoroutineScope(CoroutineName("DiscordAppender"))
        client = HttpClient(CIO)
        channel = Channel(Channel.BUFFERED)
        timeoutMillis = timeout.toLong()
        super.start()
        scope.launch {
            while (true) {
                val embed = channel.receive()
                val body = """{"embeds": [${embed.toData().toJson().decodeToString()}]}"""
                client.post(url) {
                    setBody(body)
                    contentType(ContentType.Application.Json)
                }
                delay(timeoutMillis.milliseconds)
            }
        }
    }
}

private object UnknownInteractionFilter : Filter<ILoggingEvent>() {
    override fun decide(e: ILoggingEvent): FilterReply {
        if (e.formattedMessage.contains("[ErrorResponseException] 10062")) {
            return FilterReply.DENY
        }
        return FilterReply.NEUTRAL
    }
}
