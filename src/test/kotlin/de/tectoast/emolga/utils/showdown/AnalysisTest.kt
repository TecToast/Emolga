package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.utils.json.db
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnalysisTest : FunSpec({
    test("HealStats") {
        val (sdPlayers, _) = Analysis.analyse("https://replay.pokemonshowdown.com/gen9anythinggoes-1834023491-hsy8un4j6jd8u3hlf39pkjqtwy5w8pwpw")
        val allowedHeal =
            mapOf("Florges-Blue" to 6 + 6 + 6 + 7 + 6 + 12, "Forretress" to 3, "Skeledirge" to 50).withDefault { 0 }
        sdPlayers.flatMap { it.pokemon }.forEach {
            allowedHeal.getValue(it.pokemon) shouldBe it.healed
        }
    }

    test("LeagueByGuildAdvanced") {
        val (game, _) = Analysis.analyse("https://replay.pokemonshowdown.com/gen9natdexdraft-1966767073-vgezfzthd5k88fp55413hjpddy82ofgpw")
        val gid = 815004128148979723
        val map = game.map {
            it.pokemon.map { mon ->
                Analysis.getMonName(mon.pokemon, gid)
            }
        }
        println(db.leagueByGuildAdvanced(gid, map, -1, -1))
    }
})
