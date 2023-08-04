package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.HENNY
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

private data class GetCurrentMentionData(val current: Long, val allowed: MutableSet<AllowedData>)
class LeagueTest : FunSpec({

    lateinit var league: League
    val logger = KotlinLogging.logger {}

    beforeTest { league = Default() }

    context("getCurrentMention") {
        beforeTest {
            logger.info { "setting names" }
            league.names[FLOID] = "Flo"
            league.names[HENNY] = "Henny"
        }
        fun apply(data: GetCurrentMentionData) {
            league.current = data.current
            league.allowed.clear()
            league.allowed[data.current] = data.allowed
        }
        test("only self mention") {
            apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, false))))
            league.getCurrentMention() shouldBe "<@$FLOID>"
        }
        test("both mention") {
            apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, true))))
            league.getCurrentMention() shouldBe "<@$FLOID>, ||<@$HENNY>||"
        }
        test("only other mention") {
            apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, false), AllowedData(HENNY, true))))
            league.getCurrentMention() shouldBe "**Flo**, ||<@$HENNY>||"
        }
    }

    test("names is unset if called here") {
        league.names shouldBe mutableMapOf()
    }
})
