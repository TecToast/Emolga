package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import kotlin.properties.Delegates

object EnterResult {

    private val results = mutableMapOf<Long, ResultEntry>()
    suspend fun handleStart(e: GuildCommandEvent) {
        results[e.author.idLong] = ResultEntry().apply { init(e) }
    }

    fun handleSelect(e: StringSelectInteractionEvent, userindex: String) {
        val resultEntry = results[e.user.idLong]
            ?: return e.reply_("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
                .queue()
        resultEntry.handleSelect(e, userindex)
    }

    fun handleModal(e: ModalInteractionEvent) {
        val resultEntry = results[e.user.idLong]
            ?: return e.reply_("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
                .queue()
        resultEntry.handleModal(e)
    }

    fun handleFinish(e: ButtonInteractionEvent, name: String) {
        val resultEntry = results[e.user.idLong]
            ?: return e.reply_("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
                .queue()
        when (name) {
            "check" -> {
                resultEntry.handleFinish(e)
            }

            "yes", "no" -> resultEntry.handleFinishConfirm(e, name == "yes")
        }
    }

    class ResultEntry {
        val data: List<MutableList<MonData>> = listOf(mutableListOf(), mutableListOf())
        val uids = mutableListOf<Long>()
        var league by Delegates.notNull<League>()

        private fun getPicksByUid(uid: Long) = league.providePicksForGameday(gamedayData.gameday)[uid]!!
        private suspend fun getMonsByUid(uid: Long) =
            getPicksByUid(uid).sortedWith(league.tierorderingComparator).map {
                (it.name to NameConventionsDB.convertOfficialToTL(
                    it.name,
                    league.guild
                )!!).let { (official, tl) -> SelectOption(tl, "$official#$tl") }
            }

        lateinit var picks: Map<Long, List<SelectOption>>
        lateinit var gamedayData: GamedayData


        val wifiPlayers = (0..1).map { WifiPlayer(0, false) }
        suspend fun init(e: GuildCommandEvent) {
            uids += e.author.idLong
            uids += e.arguments.getMember("opponent").idLong
            league = db.leagueByGuild(e.arguments.getNullable<Long>("guild") ?: e.guild.idLong, *uids.toLongArray())
                ?: return e.reply_(
                    "Du bist in keiner Liga mit diesem User! Wenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}!",
                    ephemeral = true
                )
            gamedayData = league.getGameplayData(uids[0], uids[1], wifiPlayers)
            buildMap {
                uids.forEach { uid ->
                    put(uid, getMonsByUid(uid))
                }
            }
            e.reply_(
                embeds = buildEmbed(), components = uids.mapIndexed { index, uid ->
                    ActionRow.of(
                        StringSelectMenu(
                            "result;$index",
                            "${if (index == 0) "Deine" else "Gegnerische"} Pokemon",
                            options = picks[uid]!!
                        )
                    )
                } + listOf(ActionRow.of(primary("resultfinish;check", "Ergebnis bestätigen"))), ephemeral = true
            )
        }

        private fun buildEmbed() = Embed {
            title = "Interaktive Ergebnis-Eingabe"
            description = "Wähle im Menü unten ein Pokemon aus!"
            data.forEachIndexed { i, map ->
                field {
                    name = "${if (i == 0) "Deine" else "Gegnerische"} Pokemon"
                    value = map.joinToString("\n")
                    inline = false
                }
            }
        }.into()

        fun handleSelect(e: StringSelectInteractionEvent, userindex: String) {
            val selected = e.values.first()
            e.replyModal(Modal("result;$userindex;$selected", "Ergebnis für ${selected.substringAfterLast("#")}") {
                short("kills", "Kills", false, placeholder = "0")
                short("dead", "Gestorben", false, placeholder = "X wenn gestorben, sonst leer lassen")
                if (data[userindex.toInt()].any { it.official == selected.substringBefore("#") }) {
                    short("remove", "Pokémon wieder rauswerfen", false, placeholder = "X wenn ja, sonst leer lassen")
                }
            }).queue()
        }

        fun handleModal(e: ModalInteractionEvent) {
            val split = e.modalId.split(";")
            val userindex = split[1].toInt()
            val (official, tl) = split[2].split("#")
            val kills = e.getValue("kills")?.asString?.toIntOrNull() ?: 0
            val dead = e.getValue("dead")?.asString?.trim()?.equals("X", true) ?: false
            val removed = e.getValue("remove")?.asString?.trim()?.equals("X", true) ?: false
            val list = data[userindex]

            list.indexOfFirst { it.official == official }.let {
                if (removed && it != -1) {
                    list.removeAt(it)
                    return@let
                }
                val monData = MonData(tl, official, kills, dead)
                if (it == -1) list.add(monData)
                else list[it] = monData
            }

            e.editMessage_(embeds = buildEmbed()).queue()
        }

        fun handleFinish(e: ButtonInteractionEvent) {
            if (checkConditionsForFinish(e)) return
            val originalComponents = e.message.components
            val buttons =
                ActionRow.of(success("resultfinish;yes", "Wirklich bestätigen"), danger("resultfinish;no", "Abbrechen"))
            val newComponents = if (originalComponents.size == 3) {
                originalComponents + listOf(buttons)
            } else originalComponents.toMutableList().apply { set(3, buttons) }
            e.editComponents(newComponents).queue()
        }

        private fun checkConditionsForFinish(e: IReplyCallback): Boolean {
            if (data[0].isEmpty() || data[1].isEmpty()) return e.reply_(
                "Du hast noch keine Daten eingeben!", ephemeral = true
            ).queue().let { true }
            if ((0..1).any { data[it].kills != data[1 - it].dead }) return e.reply_(
                "Die Kills und Tode müssen übereinstimmen!", ephemeral = true
            ).queue().let { true }
            return false
        }

        fun handleFinishConfirm(e: ButtonInteractionEvent, really: Boolean) {
            if (checkConditionsForFinish(e)) return
            if (really) {
                e.reply(generateFinalMessage()).queue()
                league.docEntry?.analyse(
                    ReplayData(
                        data.mapIndexed { index, d ->
                            wifiPlayers[index].apply {
                                alivePokemon = d.size - d.dead
                                winner = d.size != d.dead
                            }
                        },
                        uids[0],
                        uids[1],
                        data.map { it.asKillMap },
                        data.map { it.asDeathMap },
                        data.map { l -> l.map { it.official } },
                        "WIFI",
                        gamedayData
                    )
                )
            } else {
                e.editMessage_(components = e.message.components.subList(0, 3)).queue()
            }
        }

        private fun generateFinalMessage(): String {
            val spoiler = Command.spoilerTags.contains(league.guild)
            return "${
                data.mapIndexed { index, sdPlayer ->
                    mutableListOf<Any>(
                        "<@${uids[index]}>", sdPlayer.count { !it.dead }
                    ).apply { if (spoiler) add(1, "||") }.let { if (index % 2 > 0) it.asReversed() else it }
                }.joinToString(":") { it.joinToString(" ") }
            }\n\n${
                data.mapIndexed { index, monData ->
                    "<@${uids[index]}>:\n${monData.joinToString("\n")}"
                }.joinToString("\n\n")

            }"
        }

        private val List<MonData>.kills get() = sumOf { it.kills }
        private val List<MonData>.dead get() = sumOf { (if (it.dead) 1 else 0).toInt() }
        private val List<MonData>.asKillMap get() = associate { it.official to it.kills }
        private val List<MonData>.asDeathMap get() = associate { it.official to if (it.dead) 1 else 0 }

        data class MonData(val pokemon: String, val official: String, val kills: Int, val dead: Boolean) {
            override fun toString(): String {
                return "$pokemon $kills".condAppend(dead) { " X" }
            }
        }

    }
}
