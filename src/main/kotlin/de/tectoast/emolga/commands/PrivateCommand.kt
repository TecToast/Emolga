package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*
import java.util.function.Predicate

abstract class PrivateCommand(val name: String) {
    val aliases: List<String> = LinkedList()
    var isAllowed = Predicate<User> { true }

    init {
        commands.add(this)
    }

    fun setIsAllowed(isAllowed: Predicate<User>) {
        this.isAllowed = isAllowed.or { user: User -> user.idLong == Constants.FLOID }
    }

    private fun checkPrefix(msg: String): Boolean {
        return (msg.lowercase(Locale.getDefault())
            .startsWith("!" + name.lowercase(Locale.getDefault()) + " ") || aliases.stream().anyMatch { s: String ->
            msg.lowercase(Locale.getDefault()).startsWith("!" + s.lowercase(Locale.getDefault()) + " ")
        }
                || msg.equals("!" + name.lowercase(Locale.getDefault()), ignoreCase = true) || aliases.stream()
            .anyMatch { s: String ->
                msg.equals(
                    "!$s", ignoreCase = true
                )
            })
    }

    abstract fun process(e: MessageReceivedEvent)

    companion object {
        val commands: MutableList<PrivateCommand> = LinkedList()
        fun check(e: MessageReceivedEvent) {
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