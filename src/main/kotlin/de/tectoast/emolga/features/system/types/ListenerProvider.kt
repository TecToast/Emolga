package de.tectoast.emolga.features.system.types

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

/**
 * Marker for classes which are part of the feature system (at the moment Feature & ListenerProvider)
 */
abstract class ListenerProvider : KoinComponent {
    val registeredListeners: MutableSet<Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>> = mutableSetOf()

    /**
     * Registers a Listener in a feature
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : GenericEvent> registerListener(noinline listener: suspend (T) -> Unit) {
        registeredListeners += (T::class to listener) as Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>
    }

    /**
     * Registers a DM listener
     */
    fun registerDMListener(prefix: String = "", listener: suspend (MessageReceivedEvent) -> Unit) =
        registerListener<MessageReceivedEvent> {
            if (!it.author.isBot && it.channelType == ChannelType.PRIVATE && it.message.contentRaw.startsWith(prefix)) listener(
                it
            )
        }
}