package de.tectoast.emolga.utils.showdown

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
})
