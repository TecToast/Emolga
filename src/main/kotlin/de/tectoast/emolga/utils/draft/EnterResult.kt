package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import kotlin.properties.Delegates

object EnterResult {

    private val results = mutableMapOf<Long, ResultEntry>()
    suspend fun handleStart(e: GuildCommandEvent) {
        results[e.author.idLong] = ResultEntry().apply { init(e) }
    }

    // TODO
    private fun League.getPicks() = providePicksForGameday(2)

    fun handleSelect(e: StringSelectInteractionEvent, userindex: String) {
        val selected = e.values.first()
        e.replyModal(Modal("result;$userindex;$selected", "Ergebnis für ${selected.substringAfterLast("#")}") {
            short("kills", "Kills", false, placeholder = "0")
            short("dead", "Gestorben", false, placeholder = "X wenn gestorben, sonst leer lassen")
            // TODO Add a way to yeet mons
        }).queue()
    }

    fun handleModal(e: ModalInteractionEvent) {
        val resultEntry = results[e.user.idLong]
            ?: return e.reply_("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
                .queue()
        resultEntry.handleModal(e)
    }

    fun handleFinish(e: ButtonInteractionEvent) {
        val resultEntry = results[e.user.idLong]
            ?: return e.reply_("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
                .queue()
        resultEntry.handleFinish(e)
    }

    class ResultEntry {
        val data: List<MutableList<MonData>> = listOf(mutableListOf(), mutableListOf())
        val uids = mutableListOf<Long>()
        var league by Delegates.notNull<League>()

        // TODO Provide a way to get the mons by uid without blocking & remove the runBlocking & providePicksForGameday
        private suspend fun getMonsByUid(uid: Long) =
            league.getPicks()[uid]!!.sortedWith(league.tierorderingComparator).map {
                (it.name to NameConventionsDB.convertOfficialToTL(
                    it.name,
                    league.guild
                )!!).let { (official, tl) -> SelectOption(tl, "$official#$tl") }
            }

        val picks by lazy {
            runBlocking {
                buildMap {
                    uids.forEach { uid ->
                        put(uid, getMonsByUid(uid))
                    }
                }
            }
        }

        // TODO
        private fun GuildCommandEvent.provideGuild() = Constants.G.FLP

        suspend fun init(e: GuildCommandEvent) {
            uids += e.author.idLong
            uids += e.arguments.getMember("opponent").idLong
            league = db.leagueByGuild(e.provideGuild(), *uids.toLongArray()) ?: return e.reply_(
                "Du bist in keiner Liga mit diesem User! Wenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}!",
                ephemeral = true
            )
            e.reply_(
                embeds = buildEmbed(), components = uids.mapIndexed { index, uid ->
                    ActionRow.of(
                        StringSelectMenu(
                            "result;$index",
                            "${if (index == 0) "Deine" else "Gegnerische"} Pokemon",
                            options = picks[uid]!!
                        )
                    )
                } + listOf(ActionRow.of(primary("resultfinish", "Ergebnis bestätigen"))), ephemeral = true
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

        fun handleModal(e: ModalInteractionEvent) {
            val split = e.modalId.split(";")
            val userindex = split[1].toInt()
            val (official, tl) = split[2].split("#")
            val kills = e.getValue("kills")?.asString?.toIntOrNull() ?: 0
            val dead = e.getValue("dead")?.asString?.trim()?.equals("X", true) ?: false
            val list = data[userindex]
            val monData = MonData(tl, official, kills, dead)
            list.indexOfFirst { it.official == official }.let {
                if (it == -1) list.add(monData)
                else list[it] = monData
            }
            e.editMessage_(embeds = buildEmbed()).queue()
        }

        fun handleFinish(e: ButtonInteractionEvent) {
            if (data[0].isEmpty() || data[1].isEmpty()) return e.reply_(
                "Du hast noch keine Daten eingeben!", ephemeral = true
            ).queue()
            if ((0..1).any { data[it].kills != data[1 - it].dead }) return e.reply_(
                "Die Kills und Tode müssen übereinstimmen!", ephemeral = true
            ).queue()
            e.reply(generateFinalMessage()).queue()
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

        data class MonData(val pokemon: String, val official: String, val kills: Int, val dead: Boolean) {
            override fun toString(): String {
                return "$pokemon $kills".condAppend(dead) { " X" }
            }
        }

    }
}
