package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.ReplayData
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.GamedayData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.surroundWith
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import kotlin.properties.Delegates

object EnterResult {

    object ResultCommand : CommandFeature<ResultCommand.Args>(
        ::Args, CommandSpec("result", "Startet die interaktive Ergebniseingabe", Constants.G.VIP, Constants.G.COMMUNITY)
    ) {
        class Args : Arguments() {
            var opponent by member("Gegner", "Dein Gegner in diesem Kampf")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            handleStart(e.opponent.idLong)
        }
    }

    object ResWithGuild : CommandFeature<ResWithGuild.Args>(
        ::Args, CommandSpec("reswithguild", "Startet die interaktive Ergebniseingabe")
    ) {
        class Args : Arguments() {
            var guild by long("guild", "guild")
            var user by long("user", "user")
            var opponent by long("opponent", "opponent")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            handleStart(e.opponent, e.user, e.guild)
        }
    }

    object ResultMenu : SelectMenuFeature<ResultMenu.Args>(::Args, SelectMenuSpec("result")) {
        class Args : Arguments() {
            var userindex by int("index", "index").compIdOnly()
            var selected by singleOption()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val resultEntry = results[user]
                ?: return reply("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
            resultEntry.handleSelect(e)
        }
    }

    object ResultFinish : ButtonFeature<ResultFinish.Args>(::Args, ButtonSpec("resultfinish")) {
        class Args : Arguments() {
            var mode by enumBasic<Mode>("check", "check")
        }

        enum class Mode {
            CHECK, YES, NO
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val resultEntry = results[user]
                ?: return reply("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
            resultEntry.handleFinish(e)
        }
    }

    object ResultModal : ModalFeature<ResultModal.Args>(::Args, ModalSpec("result")) {
        class Args : Arguments() {
            var userindex by int("index", "index").compIdOnly()
            var selected by string("selected", "selected").compIdOnly()
            var kills by intFromString("kills", "kills") {
                modal {
                    placeholder = "0"
                }
                default = 0
            }
            var dead by createArg<String, Boolean>("dead", "dead") {
                validate { it.equals("X", ignoreCase = true) }
                modal {
                    placeholder = "X wenn gestorben, sonst leer lassen"
                }
                default = false
            }
            var remove by string<Boolean>("remove", "remove") {
                modal(modalKey = Remove) {
                    placeholder = "X wenn ja, sonst leer lassen"
                }
                validate {
                    it.equals("X", ignoreCase = true)
                }
                default = false
            }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val resultEntry = results[user]
                ?: return reply("Scheinbar wurde der Bot neugestartet, seitdem du das Menü geöffnet hast. Bitte starte die Eingabe erneut!")
            resultEntry.handleModal(e)
        }
    }

    object Remove : ModalKey


    private val results = mutableMapOf<Long, ResultEntry>()
    context(InteractionData)
    suspend fun handleStart(opponent: Long, userArg: Long? = null, guild: Long? = null) {
        val u = userArg ?: user
        val g = guild ?: gid
        val resultEntry = ResultEntry()
        if (resultEntry.init(opponent, u, g)) {
            results[user] = resultEntry
        }
    }

    class ResultEntry {
        val data: List<MutableList<MonData>> = listOf(mutableListOf(), mutableListOf())
        val uids = mutableListOf<Long>()
        var league by Delegates.notNull<League>()

        private fun getPicksByUid(uid: Long) = league.providePicksForGameday(gamedayData.gameday)[uid]!!
        private suspend fun getMonsByUid(uid: Long) = getPicksByUid(uid).sortedWith(league.tierorderingComparator).map {
            (it.name to NameConventionsDB.convertOfficialToTL(
                it.name, league.guild
            )!!).let { (official, tl) -> SelectOption(tl, "$official#$tl") }
        }

        lateinit var picks: Map<Long, List<SelectOption>>
        private lateinit var gamedayData: GamedayData


        private val wifiPlayers = (0..1).map { DraftPlayer(0, false) }
        private val defaultComponents: List<ActionRow> by lazy {
            uids.mapIndexed { index, uid ->
                ActionRow.of(ResultMenu(
                    "${if (index == 0) "Deine" else "Gegnerische"} Pokemon",
                    options = picks[uid]!!,
                ) { this.userindex = index })
            } + listOf(ActionRow.of(ResultFinish("Ergebnis bestätigen", ButtonStyle.PRIMARY) {
                mode = ResultFinish.Mode.CHECK
            }))
        }

        context(InteractionData)
        suspend fun init(opponent: Long, user: Long, guild: Long): Boolean {
            uids += user
            uids += opponent
            league = db.leagueByGuild(guild, *uids.toLongArray()) ?: return reply(
                "Du bist in keiner Liga mit diesem User! Wenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}!",
                ephemeral = true
            ).let { false }
            gamedayData = league.getGameplayData(uids[0], uids[1], wifiPlayers)
            picks = uids.associateWith { getMonsByUid(it) }
            reply(embeds = buildEmbed(), components = defaultComponents, ephemeral = true)
            return true
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

        context(InteractionData)
        fun handleSelect(e: ResultMenu.Args) {
            val selected = e.selected
            val userindex = e.userindex
            replyModal(ResultModal(
                "Ergebnis für ${selected.substringAfterLast("#")}",
                mapOf(Remove to data[userindex].any { it.official == selected.substringBefore("#") })
            ) {
                this.userindex = userindex
                this.selected = selected
            })
        }

        context(InteractionData)
        fun handleModal(e: ResultModal.Args) {
            val userindex = e.userindex
            val (official, tl) = e.selected.split("#")
            val list = data[userindex]
            list.indexOfFirst { it.official == official }.let {
                if (e.remove && it != -1) {
                    list.removeAt(it)
                    return@let
                }
                val monData = MonData(tl, official, e.kills, e.dead)
                if (it == -1) list.add(monData)
                else list[it] = monData
            }
            edit(embeds = buildEmbed())
        }

        context(InteractionData)
        suspend fun handleFinish(e: ResultFinish.Args) {
            if (checkConditionsForFinish()) return
            when (e.mode) {
                ResultFinish.Mode.CHECK -> {
                    val originalComponents = defaultComponents
                    val buttons = ActionRow.of(ResultFinish("Wirklich bestätigen", ButtonStyle.SUCCESS) {
                        mode = ResultFinish.Mode.YES
                    }, ResultFinish("Abbrechen", ButtonStyle.DANGER) { mode = ResultFinish.Mode.NO })
                    val newComponents = originalComponents + listOf(buttons)
                    edit(components = newComponents)
                }

                ResultFinish.Mode.YES -> {
                    if (league.storeInsteadSend)
                        reply(
                            "Das Ergebnis des Kampfes wurde gespeichert! Es wird dann zum Upload-Zeitpunkt im Doc veröffentlicht.",
                            ephemeral = true
                        )
                    else {
                        reply(generateFinalMessage())
                    }
                    league.docEntry?.analyse(
                        ReplayData(
                            game = data.mapIndexed { index, d ->
                                wifiPlayers[index].apply {
                                    alivePokemon = d.size - d.dead
                                    winner = d.size != d.dead
                                }
                            },
                            uids = uids,
                            kd = data.map { it.associate { p -> p.official to (p.kills to if (p.dead) 1 else 0) } },
                            mons = data.map { l -> l.map { it.official } },
                            url = "WIFI",
                            gamedayData = gamedayData.applyFun()
                        )
                    )
                }

                ResultFinish.Mode.NO -> edit(components = defaultComponents)
            }
        }

        context(InteractionData)
        private fun checkConditionsForFinish(): Boolean {
            if (data[0].isEmpty() || data[1].isEmpty()) return reply(
                "Du hast noch keine Daten eingeben!", ephemeral = true
            ).let { true }
            if ((0..1).any { data[it].kills != data[1 - it].dead }) return reply(
                "Die Kills und Tode müssen übereinstimmen!", ephemeral = true
            ).let { true }
            return false
        }

        private fun generateFinalMessage(): String {
            val spoiler = SpoilerTagsDB.contains(league.guild)
            return "${
                data.mapIndexed { index, sdPlayer ->
                    mutableListOf<Any>("<@${uids[index]}>", sdPlayer.count { !it.dead }).apply {
                        if (spoiler) add(
                            1, "||"
                        )
                    }.let { if (index % 2 > 0) it.asReversed() else it }
                }.joinToString(":") { it.joinToString(" ") }
            }\n\n${
                data.mapIndexed { index, monData ->
                    "<@${uids[index]}>:\n${monData.joinToString("\n").surroundWith(if (spoiler) "||" else "")}"
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
