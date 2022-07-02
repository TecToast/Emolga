package de.tectoast.emolga.buttons.buttonsaves

import de.tectoast.emolga.commands.Command
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.util.*

class Nominate(val mons: List<JSONObject>) {
    private val nominated: MutableList<JSONObject>
    private val notNominated: MutableList<JSONObject>

    init {
        nominated = LinkedList(mons)
        notNominated = LinkedList()
    }

    fun unnominate(name: String) {
        val o = mons.stream().filter { obj: JSONObject -> obj.getString("name") == name }.findFirst().orElse(null)
        nominated.remove(o)
        notNominated.add(o)
    }

    fun nominate(name: String) {
        val o = mons.stream().filter { obj: JSONObject -> obj.getString("name") == name }.findFirst().orElse(null)
        notNominated.remove(o)
        nominated.add(o)
    }

    private fun isNominated(s: String): Boolean {
        return nominated.stream().anyMatch { o: JSONObject -> o.getString("name") == s }
    }

    fun generateDescription(): String {
        val tiers = listOf("S", "A", "B", "C", "D")
        val msg = StringBuilder(
            """
    **Nominiert: (${nominated.size})**
    
    """.trimIndent()
        )
        for (o in tiers) {
            nominated.stream().filter { s: JSONObject -> s.getString("tier") == o }
                .map { o2: JSONObject -> o2.getString("name") }
                .sorted().forEach { mon: String -> msg.append(o).append(": ").append(mon).append("\n") }
        }
        msg.append("\n**Nicht nominiert: (").append(notNominated.size).append(")**\n")
        for (o in tiers) {
            notNominated.stream().filter { s: JSONObject -> s.getString("tier") == o }
                .map { o2: JSONObject -> o2.getString("name") }
                .sorted().forEach { mon: String -> msg.append(o).append(": ").append(mon).append("\n") }
        }
        return msg.toString()
    }

    fun render(e: ButtonInteractionEvent) {
        e.editMessageEmbeds(
            EmbedBuilder().setTitle("Nominierungen").setColor(Color.CYAN).setDescription(generateDescription()).build()
        )
            .setActionRows(Command.getActionRows(
                mons.asSequence().map { it.getString("name") }.toMutableList()
            ) {
                if (isNominated(it)) Button.primary(
                    "nominate;$it", it
                ) else Button.secondary("nominate;$it", it)
            }.toMutableList().also { it.add(ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))) })
            .queue()
    }

    private fun buildJSONString(): String {
        val tiers = listOf("S", "A", "B", "C", "D")
        val msg = StringBuilder()
        for (o in tiers) {
            nominated.stream().filter { s: JSONObject -> s.getString("tier") == o }
                .map { o2: JSONObject -> o2.getString("name") }
                .sorted().forEach { mon: String -> msg.append(mon).append(";") }
        }
        msg.setLength(msg.length - 1)
        msg.append("###")
        for (o in tiers) {
            notNominated.stream().filter { s: JSONObject -> s.getString("tier") == o }
                .map { o2: JSONObject -> o2.getString("name") }
                .sorted().forEach { mon: String -> msg.append(mon).append(";") }
        }
        msg.setLength(msg.length - 1)
        return msg.toString()
        //return nominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(",")) + "###" + notNominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(","));
    }

    fun finish(e: ButtonInteractionEvent, now: Boolean) {
        if (now) {
            val nom = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("nominations")
            val day = nom.getJSONObject(nom.getInt("currentDay").toString())
            if (day.has(e.user.id)) {
                e.reply("Du hast dein Team bereits für diesen Spieltag nominiert!").queue()
                return
            }
            day.put(e.user.id, buildJSONString())
            Command.saveEmolgaJSON()
            e.reply("Deine Nominierung wurde gespeichert!").queue()
        } else {
            if (nominated.size != 11) {
                e.reply("Du musst exakt 11 Pokemon nominieren!").setEphemeral(true).queue()
            } else {
                e.editMessageEmbeds(
                    EmbedBuilder().setTitle("Bist du dir wirklich sicher? Die Nominierung kann nicht rückgängig gemacht werden!")
                        .setColor(
                            Color.CYAN
                        ).setDescription(generateDescription()).build()
                )
                    .setActionRows(
                        ActionRow.of(
                            Button.success("nominate;FINISHNOW", "Ja"),
                            Button.danger("nominate;CANCEL", "Nein")
                        )
                    )
                    .queue()
            }
        }
    }
}