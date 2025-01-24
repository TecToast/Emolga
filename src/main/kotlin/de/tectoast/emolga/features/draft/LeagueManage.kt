package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.league.League
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq
import org.litote.kmongo.`in`

object LeagueManage {
    object LeagueManageCommand : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("leaguemanage", "Möglichkeiten zur Verwaltung einer Liga", Constants.G.VIP, Constants.G.EPP)
    ) {
        init {
            slashPrivate()
//            restrict(flo)
        }

        fun Arguments.leagueName() = fromList(
            "Liganame",
            "Der Name der Liga, die du überprüfen willst.",
            { event -> db.drafts.find(League::guild eq event.guild?.idLong).toList().map { it.leaguename } }) {
        }

        object ResultCheckCommand : CommandFeature<ResultCheckCommand.Args>(
            ::Args,
            CommandSpec("resultcheck", "Gibt aus, welche Kämpfe schon beendet sind und welche nicht."),
        ) {
            class Args : Arguments() {
                var league by leagueName()
                var gameday by int("Gameday", "Der Spieltag, den du überprüfen willst.")
                var public by boolean("Public", "Soll die Nachricht öffentlich sein?") {
                    default = false
                }
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                val league = db.getLeague(e.league) ?: return reply("Liga nicht gefunden!", ephemeral = true)
                reply(league.buildStoreStatus(e.gameday), ephemeral = !e.public)
            }

        }

        context(InteractionData)
        private suspend fun executeTipGameState() {
            deferReply(true)
            val dataList = db.tipgameuserdata.find(TipGameUserData::league `in` db.drafts.find(League::guild eq gid).toFlow().map { it.leaguename }.toList()).toList()
            val points =
                dataList.groupBy { it.user }
                    .mapValues { it.value.sumOf { d -> d.correctGuesses.values.sumOf { l -> l.size } + (if (d.correctTopkiller) 3 else 0) + d.correctOrderGuesses.sumOf { co -> 4 - co } } }
            reply(
                buildString {
                    append("Teilnehmeranzahl: ${points.size}\n\n")
                    append(points.entries.sortedByDescending { it.value }.take(10)
                        .mapIndexed { index, entry -> "${index + 1}. <@${entry.key}>: ${entry.value}" }
                        .joinToString("\n").ifEmpty { "_Keine Punkte bisher vergeben_" })
                }, ephemeral = true
            )
        }

        object TipGameStats : CommandFeature<NoArgs>(
            NoArgs(),
            CommandSpec("tipgamestats", "Zeigt den aktuellen Stand des Tippspiels an.")
        ) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                executeTipGameState()
            }
        }


        context(InteractionData) override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }
}
