package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.buttons.buttonsaves.Nominate
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

class NominateCommand : PrivateCommand("nominate") {
    private val tiercomparator: Comparator<JSONObject>

    init {
        setIsAllowed {
            Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("picks").has(it.id)
        }
        val tiers = listOf("S", "A", "B", "C", "D")
        tiercomparator = compareBy { it.getString("tier").indexedBy(tiers) }
    }

    override fun process(e: MessageReceivedEvent) {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val nom = nds.getJSONObject("nominations")
        if (nom.createOrGetJSON(nom.getInt("currentDay").toString()).has(e.author.id)) {
            e.channel.sendMessage("Du hast für diesen Spieltag dein Team bereits nominiert!").queue()
            return
        }
        val list = nds.getJSONObject("picks")
            .getJSONList(if (e.author.idLong == Constants.FLOID) Command.WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1] else e.author.id)
            .sortedWith(tiercomparator)
        val n = Nominate(list)
        e.channel.sendMessageEmbeds(
            Embed(title = "Nominierungen", color = embedColor, description = n.generateDescription())
        ).setActionRows(Command.getActionRows(list.map { it.getString("name") }) {
            Button.primary(
                "nominate;$it", it
            )
        }.toMutableList().also { it.add(ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))) })
            .queue { Command.nominateButtons[it.idLong] = n }
    }
}