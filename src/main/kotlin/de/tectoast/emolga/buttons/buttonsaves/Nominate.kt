package de.tectoast.emolga.buttons.buttonsaves

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Nominate(val mons: List<DraftPokemon>) {
    private val nominated: MutableList<DraftPokemon>
    private val notNominated: MutableList<DraftPokemon>

    init {
        nominated = ArrayList(mons)
        notNominated = ArrayList(mons.size)
    }

    fun unnominate(name: String) = mons.first { it.name == name }.let {
        nominated.remove(it)
        notNominated.add(it)
    }


    fun nominate(name: String) = mons.first { it.name == name }.let {
        notNominated.remove(it)
        nominated.add(it)
    }

    private fun isNominated(s: String) = nominated.any { it.name == s }

    companion object {
        private val tiers = listOf("S", "A", "B")
    }

    private fun List<DraftPokemon>.toMessage() = this
        .sortedWith(compareBy({ it.tier.indexedBy(tiers) }, { it.name }))
        .joinToString("\n") {
            "${it.tier}: ${it.name}"
        }

    private fun List<DraftPokemon>.toJSON() = this
        .sortedWith(compareBy({ it.tier.indexedBy(tiers) }, { it.name }))
        .joinToString(";") {
            it.name
        }

    fun generateDescription(): String {
        return buildString {
            append("**Nominiert: (${nominated.size})**\n")
            append(nominated.toMessage())
            append("\n**Nicht nominiert: (").append(notNominated.size).append(")**\n")
            append(notNominated.toMessage())
        }
    }

    fun render(e: ButtonInteractionEvent) {

        e.editMessageEmbeds(
            Embed(title = "Nominierungen", color = embedColor, description = generateDescription())
        ).setActionRows(Command.getActionRows(mons.map { it.name }) {
            if (isNominated(it)) Button.primary(
                "nominate;$it", it
            ) else Button.secondary("nominate;$it", it)
        }.toMutableList().also { it.add(ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))) })
            .queue()
    }

    private fun buildJSONString(): String {
        return buildString {
            append(nominated.toJSON())
            append("###")
            append(notNominated.toJSON())
        }
        //return nominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(",")) + "###" + notNominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(","));
    }

    fun finish(e: ButtonInteractionEvent, now: Boolean) {
        if (now) {
            /*val nom = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("nominations")
            val day = nom.getJSONObject(nom.getInt("currentDay"))
            if (day.has(e.user.id)) {
                e.reply("Du hast dein Team bereits für diesen Spieltag nominiert!").queue()
                return
            }
            day.put(e.user.id, buildJSONString())
            Command.saveEmolgaJSON()
            e.reply("Deine Nominierung wurde gespeichert!").queue()*/
            val nom = Emolga.get.nds().nominations
            val day = nom.current()
            val uid = e.user.idLong
            if (uid in day) {
                e.reply_("Du hast dein Team bereits für Spieltag ${nom.currentDay} nominiert!").queue()
                return
            }
            day[uid] = buildJSONString()
            Command.saveEmolgaJSON()
            e.reply_("Deine Nominierung wurde gespeichert!").queue()
            return
        }
        if (nominated.size != 11) {
            e.reply_(content = "Du musst exakt 11 Pokemon nominieren!", ephemeral = true).queue()
        } else {
            e.editMessage_(
                embed = Embed(
                    title = "Bist du dir wirklich sicher? Die Nominierung kann nicht rückgängig gemacht werden!",
                    color = embedColor,
                    description = generateDescription()
                ),
                components = listOf(
                    Button.success("nominate;FINISHNOW", "Ja"),
                    Button.danger("nominate;CANCEL", "Nein")
                ).into()
            )
        }
    }
}