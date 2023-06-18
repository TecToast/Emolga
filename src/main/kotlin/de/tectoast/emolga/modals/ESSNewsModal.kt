package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.file
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.text.SimpleDateFormat

class ESSNewsModal : ModalListener("essnews") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val title = e.getValue("title")!!.asString
        val text = e.getValue("text")!!.asString
        val line =
            """								<div class="newsentry"><h4>$title</h4><p>$text</p><p>&mdash;<strong>ESS Team</strong> <small class="date">am $currentDate</small></p></div>"""
        paths.forEach {
            it.writeText(it.readLines().toMutableList().apply {
                set(38, line)
            }.joinToString("\n"))
        }
        e.reply("ESS News wurden geupdated!").queue()
    }

    companion object {
        val paths = listOf(
            "/var/www/essclient/index.html".file(),
            "/var/www/essclient/index.template.html".file()
        )
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
        private val currentDate: String
            get() = dateFormat.format(System.currentTimeMillis())
    }
}
