package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object Nominate {
    private val WHITESPACES_SPLITTER = Regex("\\s+")


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
                val nomUser =
                    if (e.author.idLong == Constants.FLOID) WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1].toLong() else e.author.idLong
                val list =
                    nds.picks[nomUser]!!.map {
                        DraftPokemon(
                            NameConventionsDB.convertOfficialToTL(it.name, nds.guild)!!, it.tier, it.free
                        )
                    }
                val sortedList = list.sortedWith(tiercomparator)
                NominateState(e.author.idLong, nomUser, list, sortedList).process {
                    e.channel.sendMessage(MessageCreate(embeds = Embed(
                        title = "Nominierungen", color = embedColor, description = generateDescription()
                    ).into(), components = sortedList.map {
                        NominateButton(
                            label = it.name, buttonStyle = ButtonStyle.PRIMARY
                        ) { data = it.name; mode = Mode.UNNOMINATE }
                    }.intoMultipleRows().toMutableList().apply {
                        add(
                            ActionRow.of(NominateButton(
                                buttonStyle = ButtonStyle.SUCCESS,
                                emoji = Emoji.fromUnicode("✅"),
                                disabled = true
                            ) { mode = Mode.FINISH;data = "NOTNOW" })
                        )
                    })
                    ).queue()
                    nds.save("Nominate")
                }
            }
        }

        enum class Mode {
            NOMINATE, UNNOMINATE, FINISH, CANCEL
        }

        class Args : Arguments() {
            var mode by enumBasic<Mode>("mode", "mode")
            var data by string("data", "data")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<NominateState> {
                when (e.mode) {
                    Mode.NOMINATE -> {
                        nominate(e.data)
                        render()
                    }

                    Mode.UNNOMINATE -> {
                        unnominate(e.data)
                        render()
                    }

                    Mode.FINISH -> {
                        finish(e.data == "FINISHNOW")
                    }

                    Mode.CANCEL -> {
                        render()
                    }
                }
            }

        }
    }
}
