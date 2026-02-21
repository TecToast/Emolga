package de.tectoast.emolga.features.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.ResultCodesDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.hasRole
import de.tectoast.emolga.utils.json.emolga.reverseGet
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.translateToGuildLanguage
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object EnterResult {

    object ResultCommand : CommandFeature<ResultCommand.Args>(
        ::Args, CommandSpec(
            "result", K18n_EnterResult.ResultHelp
        )
    ) {
        private val nameCache = mutableMapOf<String, Map<Long, String>>() // guild -> uid -> name
        private val leagueCache = mutableMapOf<Long, String>()

        class Args : Arguments() {
            var opponent by fromListCommand("Opponent", K18n_EnterResult.ResultArgOpponent, {
                val gid = it.guild?.idLong
                mdb.leagueByGuild(gid ?: -1, it.user.idLong).handle(it.user.idLong, gid)
            })
        }

        private suspend fun League?.handle(user: Long, gid: Long?): Collection<String> {
            this ?: return listOf(K18n_NoLeagueForGuildFound.translateToGuildLanguage(gid))
            leagueCache[user] = leaguename
            return nameCache.getOrPut(leaguename) {
                jda.getGuildById(guild)!!.retrieveMembersByIds(table).await()
                    .associate { it.idLong to it.user.effectiveName }
            }.values
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val oppo = nameCache[leagueCache[iData.user]]?.reverseGet(e.opponent) ?: return iData.reply(
                K18n_Arguments.NotAutocompleteConform, ephemeral = true
            )
            iData.deferReply(true)
            handleStart(oppo)
        }
    }

    object ResultFor : CommandFeature<ResultFor.Args>(
        ::Args, CommandSpec(
            "resultfor", K18n_EnterResult.ResultForHelp,
        )
    ) {
        init {
            restrict {
                member().hasPermission(Permission.ADMINISTRATOR) || member().hasRole(796787738559905842)
            }
        }

        class Args : Arguments() {
            var user by member("Spieler 1", K18n_EnterResult.ResultForArgUser)
            var opponent by member("Spieler 2", K18n_EnterResult.ResultForArgOpponent)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            handleStart(e.opponent.idLong, userArg = e.user.idLong)
        }
    }

    object ResWithGuild : CommandFeature<ResWithGuild.Args>(
        ::Args, CommandSpec("reswithguild", K18n_EnterResult.ResultHelp)
    ) {
        class Args : Arguments() {
            var guild by long("guild", "guild".k18n)
            var user by long("user", "user".k18n)
            var opponent by long("opponent", "opponent".k18n)
        }

        context(_: InteractionData)
        override suspend fun exec(e: Args) {
            handleStart(e.opponent, e.user, e.guild)
        }
    }

    context(iData: InteractionData)
    suspend fun handleStart(opponent: Long, userArg: Long? = null, guild: Long? = null) {
        val u = userArg ?: iData.user
        val g = guild ?: iData.gid
//        val t = customTc ?: tc
        val league = mdb.leagueByGuild(g, u, opponent) ?: return iData.reply(
            K18n_EnterResult.NoLeagueWithOpponent(Constants.MYTAG),
            ephemeral = true
        )
        val idx1 = league(u)
        val idx2 = league(opponent)
        val gameday = league.getGamedayData(idx1, idx2).first.gameday
        val uuid = ResultCodesDB.add(league.leaguename, gameday, idx1, idx2)
        val url = "https://emolga.tectoast.de/result/${uuid}"
        iData.reply(
            K18n_EnterResult.Success(url),
            ephemeral = true
        )
    }


}
