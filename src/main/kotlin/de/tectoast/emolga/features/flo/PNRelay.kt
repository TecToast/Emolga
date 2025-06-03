package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.ListenerProvider
import de.tectoast.emolga.utils.httpClient
import de.tectoast.emolga.utils.isFlo
import de.tectoast.emolga.utils.isNotFlo
import de.tectoast.emolga.utils.surroundWith
import io.ktor.client.request.*
import io.ktor.client.statement.*

object PNRelay : ListenerProvider() {
    private const val EMOLGA_PN = 828044461379682314

    init {
        registerDMListener { e ->
            if (!e.author.isBot && e.author.isNotFlo) e.jda.getTextChannelById(EMOLGA_PN)
                ?.sendMessage(e.author.asMention + ": " + e.message.contentDisplay)?.apply {
                    if (e.message.attachments.isNotEmpty()) addContent("\n\n" + e.message.attachments.joinToString("\n") { it.url })
                }?.queue()
            if (e.author.isFlo && e.message.contentRaw.contains(",")) {
                val (replayId, lineIndexStr) = e.message.contentRaw.split(",")
                val lineIndex = lineIndexStr.toInt()
                val lines =
                    httpClient.get("https://replay.pokemonshowdown.com/${replayId}.log").bodyAsText().split("\n")
                e.channel.sendMessage(buildString {
                    for (i in lineIndex - 15..lineIndex + 5) {
                        if (i == lineIndex) append(">>> ")
                        append(lines[i])
                        if (i == lineIndex) append(" <<<")
                        append("\n")
                    }
                }.surroundWith("```")).queue()
            }
        }
    }
}
