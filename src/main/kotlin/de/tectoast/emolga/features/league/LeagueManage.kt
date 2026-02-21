package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.draft.generic.K18n_LeagueNotFound
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.mdb
import org.litote.kmongo.eq

object LeagueManage {
    object LeagueManageCommand : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("leaguemanage", K18n_LeagueManage.Help)
    ) {
        init {
            slashPrivate()
//            restrict(flo)
        }

        fun Arguments.leagueName() = fromListCommand(
            "Liganame",
            K18n_LeagueManage.ArgLeague,
            { event -> mdb.league.find(League::guild eq event.guild?.idLong).toList().map { it.leaguename } }) {
        }

        object ResultCheckCommand : CommandFeature<ResultCheckCommand.Args>(
            ::Args,
            CommandSpec("resultcheck", K18n_LeagueManage.ResultCheckHelp),
        ) {
            class Args : Arguments() {
                var league by leagueName()
                var gameday by int("Gameday", K18n_LeagueManage.ResultCheckArgGameday)
                var public by boolean("Public", K18n_LeagueManage.ResultCheckArgPublic) {
                    default = false
                }
            }

            context(iData: InteractionData)
            override suspend fun exec(e: Args) {
                League.executeOnFreshLock(
                    { mdb.getLeague(e.league) },
                    { iData.reply(K18n_LeagueNotFound, ephemeral = true) }) {
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
