package de.tectoast.emolga.utils.interactive

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

@Suppress("unused")
class Interactive(
    val tco: MessageChannel,
    val layers: List<Layer>,
    val user: User,
    val onFinish: (User, MessageChannel, Map<String, Any>) -> Unit,
    val maxtime: Int,
    val timeunit: TimeUnit,
    val timermsg: String,
    val cancelmsg: String,
    val cancelcommands: Set<String>,
    val onCancel: (Interactive) -> Unit,
    val createId: Long
) {
    var listener: Listener? = null

    lateinit var tocancel: Job


    init {
        require(maxtime >= 0) { "maxtime has to be higher or equal than 0 (value: $maxtime)" }
        tco.jda.addEventListener(Listener().also { listener = it })
        tco.sendMessage(firstUnfinishedLayer!!.msg).queue()
        startTimer()
    }

    private val firstUnfinishedLayer: Layer?
        get() = layers.firstOrNull { !it.isFinished }

    private fun finish() {
        tco.jda.removeEventListener(listener)
        val answers = layers.associate { it.id to it.answer!! }
        onFinish(user, tco, answers)
    }

    private fun startTimer() {
        tocancel = scope.launch {
            delay(timeunit.toMillis(maxtime.toLong()))
            withContext(NonCancellable) {
                tco.sendMessage(timermsg).queue()
                tco.jda.removeEventListener(listener)
                onCancel(this@Interactive)
            }
        }
    }

    inner class Listener : ListenerAdapter() {
        override fun onMessageReceived(e: MessageReceivedEvent) {
            if (e.channel.id != tco.id || e.author.id != user.id || e.messageIdLong == createId) return
            val m = e.message
            val msg = m.contentDisplay
            tocancel.cancel("New message received")
            if (cancelcommands.any { it.equals(msg, ignoreCase = true) }) {
                tco.sendMessage(cancelmsg).queue()
                tco.jda.removeEventListener(listener)
                onCancel(this@Interactive)
                return
            }
            val l: Layer = firstUnfinishedLayer ?: return
            val o: Any = l.check(m, this@Interactive)
            if (o is ErrorMessage) {
                val err = o.msg
                if (err.isNotEmpty()) tco.sendMessage(err).queue()
                if (maxtime > 0) {
                    startTimer()
                }
                return
            }
            l.answer = o
            val nl = firstUnfinishedLayer
            if (nl == null) {
                finish()
                return
            }
            var tosend = nl.msg
            for (layer in layers) {
                if (layer.isFinished) tosend = tosend.replace("{" + layer.id + "}", layer.answerAsString)
            }
            tco.sendMessage(tosend).queue()
            if (maxtime > 0) {
                startTimer()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.Default)
    }
}
