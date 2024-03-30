package de.tectoast.emolga.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.surroundWith
import dev.minn.jda.ktx.messages.Embed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.dv8tion.jda.api.entities.MessageEmbed
import kotlin.properties.Delegates

class DiscordAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    lateinit var url: String

    var timeout: String = "10000"
    var level: String = "warn"

    lateinit var scope: CoroutineScope
    lateinit var client: HttpClient
    lateinit var channel: Channel<MessageEmbed>
    val throwableConverter = ThrowableProxyConverter().apply {
        start()
    }

    var timeoutMillis by Delegates.notNull<Long>()


    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.loggerName == "de.tectoast.emolga.Main") return
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            channel.send(Embed {
                title = eventObject.loggerName.substringAfterLast(".")
                color = when (eventObject.level) {
                    Level.ERROR -> 0xFF0000
                    Level.WARN -> 0xFFA500
                    else -> embedColor
                }
                field {
                    name = "Message"
                    value = eventObject.formattedMessage.surroundWith("```")
                    inline = false
                }
                eventObject.throwableProxy?.let {
                    field {
                        name = "Exception"
                        value = throwableConverter.convert(eventObject).take(1024 - 6).surroundWith("```")
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
        scope = CoroutineScope(Dispatchers.IO + CoroutineName("DiscordAppender"))
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
                delay(timeoutMillis)
            }
        }
    }
}
