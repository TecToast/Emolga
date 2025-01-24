package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.*
import de.tectoast.emolga.league.AllowedData
import de.tectoast.emolga.league.DefaultLeague
import de.tectoast.emolga.league.DefaultLeagueSettings
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NEXT_PICK
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.M.HENNY
import de.tectoast.emolga.utils.SimpleTimer
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.json.db
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import mu.KotlinLogging

private data class GetCurrentMentionData(val current: Int, val allowed: MutableSet<AllowedData>)
class LeagueTest : FunSpec({

    lateinit var defaultLeague: League
    val logger = KotlinLogging.logger {}

    beforeTest {
        defaultLeague = DefaultLeague()
        DefaultLeagueSettings.reset()
    }

    context("DirectChecks") {
        context("getCurrentMention") {
            beforeTest {
                logger.info { "setting names" }
                defaultLeague.names.addAll(listOf("Flo", "Henny", "Emolga"))
            }
            fun apply(data: GetCurrentMentionData) {
                defaultLeague.current = data.current
                defaultLeague.allowed.clear()
                defaultLeague.allowed[data.current] = data.allowed
            }
            test("only self mention") {
                apply(GetCurrentMentionData(0, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, false))))
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>"
            }
            test("both mention") {
                apply(GetCurrentMentionData(0, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, true))))
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>, ||<@$HENNY>||"
            }
            test("only other mention") {
                apply(GetCurrentMentionData(0, mutableSetOf(AllowedData(FLOID, false), AllowedData(HENNY, true))))
                defaultLeague.getCurrentMention() shouldBe "**Flo**, ||<@$HENNY>||"
            }
            test("with only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(FLOID, true), AllowedData(
                                HENNY, true, teammate = true
                            )
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>, <@$HENNY>"
            }
            test("with teammate + other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(FLOID, true), AllowedData(
                                HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>, <@$HENNY>, ||<@723829878755164202>||"
            }
            test("only teammate and other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(FLOID, false), AllowedData(
                                HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@$HENNY>, ||<@723829878755164202>||"
            }
            test("only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(FLOID, false), AllowedData(
                                HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, false)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@$HENNY>"
            }
        }

        test("names is unset if called here") {
            defaultLeague.names.shouldBeEmpty()
        }
    }
    xcontext("Automation Checks") {
        test("Test") {
            enableReplyRedirect()
            DefaultLeagueSettings {
                duringTimerSkipMode = NEXT_PICK
            }
            createTestDraft(
                name = "Test",
                playerCount = 3,
                rounds = 2,
                hardcodedUserIds = mapOf(0 to FLOID),
                originalorder = mapOf(1 to listOf(0, 1, 2), 2 to listOf(2, 0, 1))
            )
            startTestDraft("Test")
            movePick()
            randomPick("S")
            randomPick("S")
            randomPick("S")

            randomPick("S")
            movePick()
            randomPick("S")
            randomPick("S")
            db.league("TESTTest").isRunning shouldBe false
            keepAlive()
        }
        test("StallSeconds") {
            enableReplyRedirect()
            DefaultLeagueSettings {
                timer = SimpleTimer(TimerInfo(0, 24, delayInMins = 1)).stallSeconds(20)
            }

            createTestDraft(
                name = "StallSeconds",
                playerCount = 1,
                rounds = 2
            )
            startTestDraft("StallSeconds")
            logger.info { "Started" }
            delay(70000)
            logger.info { "Delay is over" }
            randomPick("S")
//            db.league("TESTStallSeconds").usedStallSeconds.values.sum() shouldBe 10

            keepAlive()
        }
    }
    xtest("CreateDefaultTestLeague") {
        createTestDraft(
            "ASL",
            3,
            0,
            originalorder = emptyMap(),
            hardcodedUserIds = mapOf(0 to FLOID, 1 to 694543579414134802, 2 to HENNY)
        )
    }
})
