package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*
import java.util.function.Predicate

abstract class PrivateCommand(val name: String) {
    val aliases: List<String> = mutableListOf()
    var isAllowed = Predicate<User> { true }

    init {
        commands.add(this)
    }

    fun setIsAllowed(isAllowed: Predicate<User>) {
        this.isAllowed = isAllowed.or { it.idLong == Constants.FLOID }
    }

    private fun checkPrefix(msg: String): Boolean {
        return (msg.lowercase().startsWith("!" + name.lowercase() + " ") || aliases.any {
            msg.lowercase().startsWith("!" + it.lowercase() + " ")
        } || msg.equals("!" + name.lowercase(), ignoreCase = true) || aliases.any {
            msg.equals(
                "!$it", ignoreCase = true
            )
        })
    }

    abstract suspend fun process(e: MessageReceivedEvent)

    companion object {
        val commands: MutableList<PrivateCommand> = LinkedList()
        suspend fun check(e: MessageReceivedEvent) {
            val m = e.message
            val u = e.author
            val msg = m.contentDisplay
            for (c in commands) {
                if (!c.checkPrefix(msg)) continue
                if (!c.isAllowed.test(u)) continue
                c.process(e)
            }
        }
    }
}
