package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.litote.kmongo.*

object Nominate {
    val nominateButtons: MutableMap<Long, Nominate> = HashMap()
    private val WHITESPACES_SPLITTER = Regex("\\s+")

    class Nominate(private val originalMons: List<DraftPokemon>, val mons: List<DraftPokemon>) {
        private val nominated: MutableList<DraftPokemon> = ArrayList(mons)
        private val notNominated: MutableList<DraftPokemon> = ArrayList(mons.size)

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
            val comparator = compareBy<DraftPokemon>({ it.tier.indexedBy(tiers) }, { it.name })
        }

        private fun List<DraftPokemon>.toMessage(): String {
            return this.sortedWith(comparator).joinToString("\n") {
                "${it.tier}: ${it.name}"
            }
        }

        private fun List<DraftPokemon>.toJSON() = this.sortedWith(comparator).joinToString(";") {
            it.indexedBy(originalMons).toString()
        }

        fun generateDescription(): String {
            return buildString {
                append("**Nominiert: (${nominated.size})**\n")
                append(nominated.toMessage())
                append("\n**Nicht nominiert: (").append(notNominated.size).append(")**\n")
                append(notNominated.toMessage())
            }
        }

        context(InteractionData)
        fun render() {

            edit(embeds = Embed(
                title = "Nominierungen", color = embedColor, description = generateDescription()
            ).into(), components = mons.map {
                val s = it.name
                NominateButton(s, if (isNominated(s)) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY) { data = s }
            }.intoMultipleRows().toMutableList().apply {
                add(
                    ActionRow.of(NominateButton(
                        buttonStyle = ButtonStyle.SUCCESS,
                        emoji = Emoji.fromUnicode("✅"),
                        disabled = nominated.size != 11
                    ) { data = "FINISH" })
                )
            })
        }

        private fun buildJSONString(): String {
            return buildString {
                append(nominated.toJSON())
                append(";")
                append(notNominated.toJSON())
            }
        }

        context(InteractionData)
        suspend fun finish(now: Boolean) {
            if (now) {
                val nom = db.nds().nominations
                val day = nom.current()
                if (user in day) return reply("Du hast dein Team bereits für Spieltag ${nom.currentDay} nominiert!")
                db.drafts.updateOne(
                    League::leaguename eq "NDS", set(
                        (NDS::nominations / Nominations::nominated).keyProjection(nom.currentDay)
                            .keyProjection(user) setTo buildJSONString()
                    )
                )
                return reply("Deine Nominierung wurde gespeichert!")
            }
            if (nominated.size != 11) {
                reply(content = "Du musst exakt 11 Pokemon nominieren!", ephemeral = true)
            } else {
                edit(
                    embeds = Embed(
                        title = "Bist du dir wirklich sicher? Die Nominierung kann nicht rückgängig gemacht werden!",
                        color = embedColor,
                        description = generateDescription()
                    ).into(), components = listOf(
                        Button.success("nominate;FINISHNOW", "Ja"), Button.danger("nominate;CANCEL", "Nein")
                    ).into()
                )
            }
        }
    }

    object NominateButton : ButtonFeature<NominateButton.Args>(::Args, ButtonSpec("nominate")) {

        private val tiercomparator: Comparator<DraftPokemon> =
            compareBy({ it.tier.indexedBy(listOf("S", "A", "B", "C", "D")) }, { it.name })


        override val label = ""
        override val buttonStyle = ButtonStyle.PRIMARY

        init {
            registerPNListener("!nominate") { e ->
                val nds = db.nds()
                val nom = nds.nominations
                if (e.author.idLong in nom.nominated.getOrPut(nom.currentDay) { mutableMapOf() }) {
                    return@registerPNListener e.channel.sendMessage("Du hast für diesen Spieltag dein Team bereits nominiert!")
                        .queue()
                }
                val list =
                    nds.picks[if (e.author.idLong == Constants.FLOID) WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1].toLong() else e.author.idLong]!!.map {
                        DraftPokemon(
                            NameConventionsDB.convertOfficialToTL(it.name, nds.guild)!!, it.tier, it.free
                        )
                    }
                val sortedList = list.sortedWith(tiercomparator)
                val n = Nominate(list, sortedList)
                e.channel.sendMessage(MessageCreate(embeds = Embed(
                    title = "Nominierungen", color = embedColor, description = n.generateDescription()
                ).into(), components = list.map {
                    NominateButton(
                        label = it.name, buttonStyle = ButtonStyle.PRIMARY
                    ) { data = it.name }
                }.intoMultipleRows().toMutableList().apply {
                    add(
                        ActionRow.of(NominateButton(
                            buttonStyle = ButtonStyle.SUCCESS,
                            emoji = Emoji.fromUnicode("✅"),
                            disabled = true
                        ) { data = "FINISH" })
                    )
                })
                ).queue { nominateButtons[it.idLong] = n }
                nds.save()
            }
        }

        enum class Mode {
            NOMINATE, UNNOMINATE, FINISH
        }

        class Args : Arguments() {
            var mode by enumBasic<Mode>("mode", "mode")
            var data by string("data", "data")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val n = nominateButtons[user] ?: return reply("Diese Nachricht ist veraltet! Nutze erneut `!nominate`!")
            when (e.mode) {
                Mode.NOMINATE -> {
                    n.nominate(e.data)
                    n.render()
                }

                Mode.UNNOMINATE -> {
                    n.unnominate(e.data)
                    n.render()
                }

                Mode.FINISH -> {
                    n.finish(e.data == "FINISHNOW")
                }
            }
        }
    }
}
