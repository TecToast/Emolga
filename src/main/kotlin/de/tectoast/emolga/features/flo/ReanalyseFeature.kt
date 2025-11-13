package de.tectoast.emolga.features.flo

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.ktor.KD
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.ReplayData
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.json.db

object ReanalyseFeature : MessageContextFeature(MessageContextSpec("Reanalyse (DEV ONLY)")) {
    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        iData.deferReply(true)
        val message = e.message.embeds.first().description!!
        val userRegex = Regex("<@(\\d+)>.*?<@(\\d+)")
        val uids = userRegex.find(message)!!.groupValues.drop(1).map { it.toLong() }
        val league = db.leaguesByGuild(iData.gid, *uids.toLongArray()).single()
        val idxs = uids.map { league(it) }
        val lines = message.split("\n")
        val singleGame = listOf(lines.subList(3, 9), lines.subList(11, 17)).map { playerLines ->
            playerLines.associate {
                val split = it.replace("|", "").split(" ")
                split[0] to KD(split[1].toInt(), split.getOrNull(2) == "X")
            }
        }
        League.executeOnFreshLock(league.leaguename) {
            val gamedayData = getGamedayData(idxs[0], idxs[1], (0..1).map { DraftPlayer(0, false) })
            val officialNameCache = mutableMapOf<String, String>()
            val game = singleGame.map { d ->
                val dead = d.count { it.value.d }
                DraftPlayer(alivePokemon = d.size - dead, winner = d.size != dead)
            }
            val replayData = ReplayData(
                game = game,
                uindices = idxs,
                kd = singleGame.map { p ->
                    p.map {
                        NameConventionsDB.getDiscordTranslation(
                            it.key,
                            guild
                        )!!.official.also { official ->
                            officialNameCache[it.key] = official
                        } to (it.value.k to if (it.value.d) 1 else 0)
                    }.toMap()
                },
                mons = singleGame.map { l -> l.map { officialNameCache[it.key]!! } },
                url = "WIFI",
                gamedayData = gamedayData.apply {
                    numbers = game.map { it.alivePokemon }
                        .let { l -> if (gamedayData.u1IsSecond) l.reversed() else l }
                })
            docEntry?.analyse(listOf(replayData))
        }
        iData.reply("Reanalyse finished", ephemeral = true)
    }
}
