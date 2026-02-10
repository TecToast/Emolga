package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.db
import org.litote.kmongo.eq

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
        override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }
}
