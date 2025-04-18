package de.tectoast.emolga.league

import de.tectoast.emolga.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.SimpleTimer
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.json.db
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import mu.KotlinLogging

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
                defaultLeague.currentOverride = data.current
                defaultLeague.allowed.clear()
                defaultLeague.allowed[data.current] = data.allowed
            }
            test("only self mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(Constants.FLOID, true), AllowedData(Constants.M.HENNY, false))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${Constants.FLOID}>"
            }
            test("both mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(Constants.FLOID, true), AllowedData(Constants.M.HENNY, true))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${Constants.FLOID}>, ||<@${Constants.M.HENNY}>||"
            }
            test("only other mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(Constants.FLOID, false), AllowedData(Constants.M.HENNY, true))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, ||<@${Constants.M.HENNY}>||"
            }
            test("with only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(Constants.FLOID, true), AllowedData(
                                Constants.M.HENNY, true, teammate = true
                            )
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${Constants.FLOID}>, <@${Constants.M.HENNY}>"
            }
            test("with teammate + other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(Constants.FLOID, true), AllowedData(
                                Constants.M.HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${Constants.FLOID}>, <@${Constants.M.HENNY}>, ||<@723829878755164202>||"
            }
            test("only teammate and other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(Constants.FLOID, false), AllowedData(
                                Constants.M.HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@${Constants.M.HENNY}>, ||<@723829878755164202>||"
            }
            test("only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(Constants.FLOID, false), AllowedData(
                                Constants.M.HENNY, true, teammate = true
                            ), AllowedData(723829878755164202, false)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@${Constants.M.HENNY}>"
            }
        }

        test("names is unset if called here") {
            defaultLeague.names.shouldBeEmpty()
        }
    }
    xcontext("Automation Checks") {
        test("Test") {
            // NOT WORKING WITH NEW SAVE SYSTEM
            enableReplyRedirect()
            DefaultLeagueSettings {
                duringTimerSkipMode = NEXT_PICK
            }
            createTestDraft(
                name = "Test",
                playerCount = 3,
                rounds = 2,
                hardcodedUserIds = mapOf(0 to Constants.FLOID),
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
            hardcodedUserIds = mapOf(0 to Constants.FLOID, 1 to 694543579414134802, 2 to Constants.M.HENNY)
        )
    }
})

private data class GetCurrentMentionData(val current: Int, val allowed: MutableSet<AllowedData>)
