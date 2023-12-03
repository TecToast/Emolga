package de.tectoast.emolga.utils.interactive

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.concurrent.TimeUnit


@Suppress("unused")
class InteractiveTemplate(
    private val onFinish: (User, MessageChannel, Map<String, Any>) -> Unit,
    private val cancelmsg: String
) {
    private val layers = mutableListOf<Layer>()
    private val cancelcommands = mutableSetOf<String>()
    private val skip = mutableMapOf<String, (User, MessageChannel) -> Boolean>()
    private var maxtime = 0
    private var timeunit = TimeUnit.SECONDS
    private var timermsg: String? = null
    private var onCancel: (Interactive) -> Unit = {}


    fun addLayer(
        id: String,
        msg: String,
        check: (Message, Interactive) -> Any,
        toString: (Any) -> String = Any::toString
    ) = apply {
        layers.add(Layer(id, msg, check, toString))
    }

    fun addCancelCommand(cmd: String) = apply {
        cancelcommands.add(cmd)
    }

    fun setTimer(maxtime: Int, timeunit: TimeUnit, timermsg: String) = apply {
        this.maxtime = maxtime
        this.timeunit = timeunit
        this.timermsg = timermsg
    }

    fun setSkip(id: String, check: (User, MessageChannel) -> Boolean) = apply {
        skip[id] = check
    }

    fun createInteractive(user: User, tco: MessageChannel, createId: Long) {


        Interactive(tco,
            layers.filter { skip[it.id]?.invoke(user, tco) != false }.map { it.copy() },
            user,
            onFinish,
            maxtime,
            timeunit,
            timermsg
                ?: "Die Zeit ist abgelaufen!",
            cancelmsg,
            cancelcommands,
            onCancel,
            createId
        )
    }

    fun setOnCancel(onCancel: (Interactive) -> Unit) = apply {
        this.onCancel = onCancel
    }
}
