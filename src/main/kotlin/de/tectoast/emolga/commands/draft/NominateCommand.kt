package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.buttons.buttonsaves.Nominate
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.util.stream.Collectors

class NominateCommand : PrivateCommand("nominate") {
    private val tiercomparator: Comparator<JSONObject>

    init {
        setIsAllowed { u: User ->
            Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("picks").has(u.id)
        }
        val tiers = listOf("S", "A", "B", "C", "D")
        tiercomparator = Comparator.comparing { o: JSONObject -> tiers.indexOf(o.getString("tier")) }
    }

    override fun process(e: MessageReceivedEvent) {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val nom = nds.getJSONObject("nominations")
        val currentDay = nom.getInt("currentDay")
        if (!nom.has(currentDay)) nom.put(currentDay, JSONObject())
        if (nom.getJSONObject(currentDay.toString()).has(e.author.id)) {
            e.channel.sendMessage("Du hast für diesen Spieltag dein Team bereits nominiert!").queue()
            return
        }
        val arr = nds.getJSONObject("picks")
            .getJSONArray(if (e.author.idLong == Constants.FLOID) Command.WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1] else e.author.id)
        val list = arr.toJSONList()
        list.sortWith(tiercomparator)
        val b = list.stream().map { o: JSONObject? -> o!!.getString("name") }.collect(Collectors.toList())
        val n = Nominate(list)
        e.channel.sendMessageEmbeds(
            EmbedBuilder().setTitle("Nominierungen").setColor(Color.CYAN).setDescription(n.generateDescription())
                .build()
        )
            .setActionRows(Command.getActionRows(b) {
                Button.primary(
                    "nominate;$it", it
                )
            }.toMutableList().also { it.add(ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))) })
            .queue { m: Message -> Command.nominateButtons[m.idLong] = n }
    }
}