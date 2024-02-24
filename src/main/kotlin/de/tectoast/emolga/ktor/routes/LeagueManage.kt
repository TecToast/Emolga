package de.tectoast.emolga.ktor.routes

import de.tectoast.emolga.ktor.userId
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.contains

fun Route.leagueManage() {
    get("/myleagues") {
        val u = userId()
        val send = db.drafts.find(League::table contains u).toList().map {
            LeagueData(
                it.leaguename,
                it.providePicksForGameday(1)[u]!!,
                it.isRunning,
                "https://cdn.discordapp.com/icons/518008523653775366/2f9ba8f68d7b468f5945c54fcb61c46a.png"
            )
        }.sortedByDescending { it.isRunning }
        call.respond(send)
    }
}

@Serializable
data class LeagueData(val name: String, val picks: List<DraftPokemon>, val isRunning: Boolean, val logoUrl: String)
