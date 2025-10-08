package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.ResultCodesDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.hasRole
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.reverseGet
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission

object EnterResult {

    object ResultCommand : CommandFeature<ResultCommand.Args>(
        ::Args, CommandSpec(
            "result", "Startet die interaktive Ergebniseingabe"
        )
    ) {
        private val nameCache = mutableMapOf<String, Map<Long, String>>() // guild -> uid -> name
        private val leagueCache = mutableMapOf<Long, String>()

        class Args : Arguments() {
            var opponent by fromList("Gegner", "Dein Gegner", {
                db.leagueByGuild(it.guild?.idLong ?: -1, it.user.idLong).handle(it.user.idLong)
            })
        }

        private suspend fun League?.handle(user: Long): Collection<String> {
            this ?: return listOf("Du bist in keiner Liga auf diesem Server!")
            leagueCache[user] = leaguename
            return nameCache.getOrPut(leaguename) {
                jda.getGuildById(guild)!!.retrieveMembersByIds(table).await()
                    .associate { it.idLong to it.user.effectiveName }
            }.values
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val oppo = nameCache[leagueCache[iData.user]]?.reverseGet(e.opponent) ?: return iData.reply(
                "Gegner wurde nicht gefunden, hast du dich an die Autovervollständigung gehalten?", ephemeral = true
            )
            iData.deferReply(true)
            handleStart(oppo)
        }
    }

    object ResultFor : CommandFeature<ResultFor.Args>(
        ::Args, CommandSpec(
            "resultfor", "Startet die interaktive Ergebniseingabe für jemand anderen",
        )
    ) {
        init {
            restrict {
                member().hasPermission(Permission.ADMINISTRATOR) || member().hasRole(796787738559905842)
            }
        }

        class Args : Arguments() {
            var user by member("Spieler 1", "Spieler 1")
            var opponent by member("Spieler 2", "Spieler 2")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            handleStart(e.opponent.idLong, userArg = e.user.idLong)
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
        val league = db.leagueByGuild(g, u, opponent) ?: return iData.reply(
            "Du bist in keiner Liga mit diesem User! Wenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}!",
            ephemeral = true
        )
        val idx1 = league(u)
        val idx2 = league(opponent)
        val gameday = league.getGamedayData(idx1, idx2, List(2) { DraftPlayer(0, false) }).gameday
        val uuid = ResultCodesDB.add(league.leaguename, gameday, idx1, idx2)
        iData.reply(
            "Bitte trage das Ergebnis auf meiner Website ein:\nhttps://emolga.tectoast.de/result/${uuid}",
            ephemeral = true
        )
    }


}
