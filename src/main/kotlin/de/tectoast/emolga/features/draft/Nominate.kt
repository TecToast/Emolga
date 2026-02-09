package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

object Nominate {
    private val WHITESPACES_SPLITTER = Regex("\\s+")


    object NominateButton : ButtonFeature<NominateButton.Args>(::Args, ButtonSpec("nominate")) {

        private val tiercomparator: Comparator<DraftPokemon> =
            compareBy({ it.tier.indexedBy(listOf("S+", "S", "A", "B", "C", "D")) }, { it.name })

        init {
            restrict { user in db.nds().table }
        }


        override val label = EmptyMessage

        init {
            registerDMListener("!nominate") { e ->
                val uid = e.author.idLong
                val language = K18N_DEFAULT_LANGUAGE
                League.executeOnFreshLock({ db.nds() }) l@{
                    val nds = this as NDS
                    if (uid != Constants.FLOID && uid !in nds.table) return@l
                    val nom = nds.nominations
                    val nomUser =
                        if (uid == Constants.FLOID) WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1].toInt() else nds(
                            uid
                        )
                    if (nomUser in nom.nominated.getOrPut(nom.currentDay) { mutableMapOf() }) {
                        return@l e.channel.sendMessage(
                            K18n_Nominate.AlreadyNominated(nom.currentDay).translateTo(language)
                        )
                            .queue()
                    }
                    val list =
                        nds.picks[nomUser]!!.filter { !it.quit }.map {
                            DraftPokemon(
                                NameConventionsDB.convertOfficialToTL(it.name, nds.guild)!!, it.tier, it.free
                            )
                        }
                    val sortedList = list.sortedWith(tiercomparator)
                    NominateState(uid, nomUser, list, sortedList).process {
                        e.channel.sendMessage(
                            MessageCreate(
                                embeds = Embed(
                                    title = K18n_Nominate.EmbedTitle.translateTo(language),
                                    color = embedColor,
                                    description = generateDescription()
                                ).into(), components = sortedList.map {
                                    withoutIData(
                                        language = language,
                                        label = it.name.k18n, buttonStyle = ButtonStyle.PRIMARY
                                    ) { data = it.name; mode = Mode.UNNOMINATE }
                                }.intoMultipleRows().toMutableList().apply {
                                    add(
                                        ActionRow.of(
                                            withoutIData(
                                                language = language,
                                                buttonStyle = ButtonStyle.SUCCESS,
                                                emoji = Emoji.fromUnicode("✅"),
                                                disabled = true
                                            ) { mode = Mode.FINISH; data = "NOTNOW" })
                                    )
                                })
                        ).queue()
                        nds.save()
                    }
                }
            }
        }

        enum class Mode {
            NOMINATE, UNNOMINATE, FINISH, CANCEL
        }

        class Args : Arguments() {
            var mode by enumBasic<Mode>()
            var data by string()
        }

        context(iData: InteractionData)
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
