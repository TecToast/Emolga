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
            league.names[723829878755164202] = "Emolga"
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
        test("with only teammate") {
            apply(
                GetCurrentMentionData(
                    FLOID, mutableSetOf(
                        AllowedData(FLOID, true), AllowedData(
                            HENNY, true,
                            teammate = true
                        )
                    )
                )
            )
            league.getCurrentMention() shouldBe "<@$FLOID>, <@$HENNY>"
        }
        test("with teammate + other") {
            apply(
                GetCurrentMentionData(
                    FLOID, mutableSetOf(
                        AllowedData(FLOID, true), AllowedData(
                            HENNY, true,
                            teammate = true
                        ), AllowedData(723829878755164202, true)
                    )
                )
            )
            league.getCurrentMention() shouldBe "<@$FLOID>, <@$HENNY>, ||<@723829878755164202>||"
        }
        test("only teammate and other") {
            apply(
                GetCurrentMentionData(
                    FLOID, mutableSetOf(
                        AllowedData(FLOID, false), AllowedData(
                            HENNY, true,
                            teammate = true
                        ), AllowedData(723829878755164202, true)
                    )
                )
            )
            league.getCurrentMention() shouldBe "**Flo**, <@$HENNY>, ||<@723829878755164202>||"
        }
        test("only teammate") {
            apply(
                GetCurrentMentionData(
                    FLOID, mutableSetOf(
                        AllowedData(FLOID, false), AllowedData(
                            HENNY, true,
                            teammate = true
                        ), AllowedData(723829878755164202, false)
                    )
                )
            )
            league.getCurrentMention() shouldBe "**Flo**, <@$HENNY>"
        }
    }

    test("names is unset if called here") {
        league.names shouldBe mutableMapOf()
    }
})
