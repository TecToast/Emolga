package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class CounterButton : ButtonListener("counter") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val split = name.split(":".toRegex())
        val method = split[0]
        val mem = e.member!!
        val counter = Command.shinycountjson.getJSONObject("counter")
        val id = if (mem.id == "893773494578470922") "598199247124299776" else mem.id
        counter.getJSONObject(method).put(id, counter.getJSONObject(method).optInt(id, 0) + split[1].toInt())
        Command.updateShinyCounts(e)
    }
}