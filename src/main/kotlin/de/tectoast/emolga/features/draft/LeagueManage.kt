package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq
import org.litote.kmongo.`in`

object LeagueManage {
    object LeagueManageCommand : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("leaguemanage", "Möglichkeiten zur Verwaltung einer Liga")
    ) {
        init {
            slashPrivate()
//            restrict(flo)
        }

        fun Arguments.leagueName() = fromList(
            "Liganame",
            "Der Name der Liga, die du überprüfen willst.",
            { event -> db.league.find(League::guild eq event.guild?.idLong).toList().map { it.leaguename } }) {
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

            context(iData: InteractionData)
            override suspend fun exec(e: Args) {
                League.executeOnFreshLock(
                    { db.getLeague(e.league) },
                    { iData.reply("Liga nicht gefunden!", ephemeral = true) }) {
                    iData.reply(buildStoreStatus(e.gameday), ephemeral = !e.public)
                }
            }

        }

        context(iData: InteractionData)
        private suspend fun executeTipGameState() {
            iData.deferReply(true)
            val dataList = db.tipgameuserdata.find(
                TipGameUserData::league `in` db.league.find(League::guild eq iData.gid).toFlow().map { it.leaguename }
                    .toList()
            ).toList()
            val points =
                dataList.groupBy { it.user }
                    .mapValues { it.value.sumOf { d -> d.correctGuesses.values.sumOf { l -> l.size } + (if (d.correctTopkiller) 3 else 0) + d.correctOrderGuesses.sumOf { co -> 4 - co } } }
            iData.reply(
                buildString {
                    append("Teilnehmeranzahl: ${points.size}\n\n")
                    append(
                        points.entries.sortedByDescending { it.value }.take(10)
                            .mapIndexed { index, entry -> "${index + 1}. <@${entry.key}>: ${entry.value}" }
                            .joinToString("\n").ifEmpty { "_Keine Punkte bisher vergeben_" })
                }, ephemeral = true
            )
        }

        object TipGameStats : CommandFeature<NoArgs>(
            NoArgs(),
            CommandSpec("tipgamestats", "Zeigt den aktuellen Stand des Tippspiels an.")
        ) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                executeTipGameState()
            }
        }


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }
}
