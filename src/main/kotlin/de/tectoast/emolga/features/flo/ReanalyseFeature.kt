package de.tectoast.emolga.features.flo

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.FullGameData
import de.tectoast.emolga.utils.KD
import de.tectoast.emolga.utils.ReplayData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.reversedIf

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
                NameConventionsDB.getDiscordTranslation(
                    split[0],
                    iData.gid,
                )!!.official to KD(split[1].toInt(), split.getOrNull(2)?.count { c -> c == 'X' } ?: 0)
            }
        }
        League.executeOnFreshLock(league.leaguename) {
            val (gamedayData, u1IsSecond) = getGamedayData(idxs[0], idxs[1])
            val fullGameData = FullGameData(
                uindices = idxs.reversedIf(u1IsSecond),
                gamedayData = gamedayData,
                games = listOf(
                    ReplayData(
                        singleGame,
                        winnerIndex = singleGame.reversedIf(u1IsSecond)
                            .indexOfFirst { p -> p.values.sumOf { it.deaths } < p.size },
                        url = "N/A"
                    )
                ),
            )
            docEntry?.analyse(fullGameData)
        }
        iData.reply("Reanalyse finished", ephemeral = true)
    }
}
