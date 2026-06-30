package de.tectoast.emolga.domain.league.gamedata.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LeagueEventSpecificData {

    fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>)

    sealed class Sanction : LeagueEventSpecificData {
        abstract val reason: String
        abstract val issuer: Long
    }

    @Serializable
    @SerialName("MatchResult")
    data class MatchResult(
        val data: List<Int>
    ) : LeagueEventSpecificData {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            for ((i, idx) in event.uindices.withIndex()) {
                map[idx]!!.let {
                    val k = data[i]
                    val d = data[1 - i]
                    it.kills += k
                    it.deaths += d
                    it.points += if (k > d) 3 else 0
                    it.wins += if (k > d) 1 else 0
                    it.losses += if (k < d) 1 else 0
                }
            }
        }
    }


    @Serializable
    @SerialName("Zeroed")
    class ZeroedGame(
        override val reason: String,
        override val issuer: Long,
    ) : Sanction() {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            event.uindices.forEach { idx ->
                map[idx]?.let { data ->
                    data.deaths += 6
                }
            }
        }
    }

    @Serializable
    @SerialName("PointPenalty")
    data class PointPenalty(
        val amount: Int,
        override val reason: String,
        override val issuer: Long,
    ) : Sanction() {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            event.uindices.forEach { idx ->
                map[idx]?.let { data ->
                    data.points -= amount
                }
            }
        }
    }
}