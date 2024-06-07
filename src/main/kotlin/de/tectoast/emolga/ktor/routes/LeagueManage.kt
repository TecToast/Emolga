package de.tectoast.emolga.ktor.routes

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import de.tectoast.emolga.utils.universalLogger
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.contains

fun Route.leagueManage() {
    get("/myleagues") {
        val u = /*userId()*/ 694543579414134802
        val send = db.drafts.find(League::table contains u).toList().map {
            LeagueData(
                it.leaguename,
                it.providePicksForGameday(1)[it(u)]!!.map { mon ->
                    val draftName = NameConventionsDB.convertOfficialToTLFull(mon.name, it.guild, plusOther = true)!!
                    val sdName = draftName.otherOfficial!!.toSDName()
                    val pkmn = db.pokedex.get(sdName)!!
                    AdvancedPokemonData(
                        draftName.tlName,
                        pkmn.spriteName,
                        mon.tier,
                        pkmn.types
                    )
                },
                it.isRunning,
                "https://cdn.discordapp.com/icons/518008523653775366/2f9ba8f68d7b468f5945c54fcb61c46a.png"
            )
        }.sortedByDescending { it.isRunning }
        universalLogger.info("Sending $send")
        call.respond(send)
    }
}

@Serializable
data class LeagueData(
    val name: String,
    val picks: List<AdvancedPokemonData>,
    val isRunning: Boolean,
    val logoUrl: String
)

@Serializable
data class AdvancedPokemonData(
    val displayName: String,
    val sdName: String,
    val pointsOrTier: String,
    val types: List<String>
)
