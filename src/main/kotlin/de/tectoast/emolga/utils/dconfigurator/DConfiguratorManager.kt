package de.tectoast.emolga.utils.dconfigurator

import dev.minn.jda.ktx.events.listener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object DConfiguratorManager : ListenerAdapter() {
    private val activeConfigurators: MutableMap<String, DConfigurator> = mutableMapOf()
    // KeyFormat: userid;tcid

    fun registerEvent(jda: JDA) {
        jda.listener<GenericInteractionCreateEvent> {
            activeConfigurators[it.configuratorId]?.handle(it)
        }
    }

    operator fun get(userId: Long, channelId: Long) = activeConfigurators["$userId;$channelId"]

    fun addConfigurator(configurator: DConfigurator, userId: Long, channelId: Long) {
        activeConfigurators["$userId;$channelId"] = configurator
    }

    fun removeConfigurator(userId: Long, channelId: Long) {
        activeConfigurators.remove("$userId;$channelId")
    }

    private val GenericInteractionCreateEvent.configuratorId get() = "${user.idLong};${channel!!.idLong}"
}
