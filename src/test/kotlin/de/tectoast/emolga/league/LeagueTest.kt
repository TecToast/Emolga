package de.tectoast.emolga.league

import de.tectoast.emolga.utils.Constants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import mu.KotlinLogging

class LeagueTest : FunSpec({

    val u1 = Constants.FLOID
    val u2 = 723829878755164202
    val u3 = 1119738272692842599

    lateinit var defaultLeague: League
    val logger = KotlinLogging.logger {}



    context("DirectChecks") {
        beforeTest {
            defaultLeague = spyk(DefaultLeague())
            every { defaultLeague[0] } returns u1
            every { defaultLeague[1] } returns u2
            every { defaultLeague[2] } returns u3
            DefaultLeagueSettings.reset()
            logger.info { "setting names" }
            defaultLeague.names.addAll(listOf("Flo", "Emolga", "EmolgaTesting"))
        }
        context("getCurrentMention") {
            fun apply(data: GetCurrentMentionData) {
                defaultLeague.currentOverride = data.current
                defaultLeague.allowed.clear()
                defaultLeague.allowed[data.current] = data.allowed
            }
            test("only self mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(u1, true), AllowedData(u2, false))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${u1}>"
            }
            test("both mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(u1, true), AllowedData(u2, true))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${u1}>, ||<@${u2}>||"
            }
            test("only other mention") {
                apply(
                    GetCurrentMentionData(
                        0,
                        mutableSetOf(AllowedData(u1, false), AllowedData(u2, true))
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, ||<@${u2}>||"
            }
            test("with only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(u1, true), AllowedData(
                                u2, true, teammate = true
                            )
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${u1}>, <@${u2}>"
            }
            test("with teammate + other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(u1, true), AllowedData(
                                u2, true, teammate = true
                            ), AllowedData(u3, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "<@${u1}>, <@${u2}>, ||<@${u3}>||"
            }
            test("only teammate and other") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(u1, false), AllowedData(
                                u2, true, teammate = true
                            ), AllowedData(u3, true)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@${u2}>, ||<@${u3}>||"
            }
            test("only teammate") {
                apply(
                    GetCurrentMentionData(
                        0, mutableSetOf(
                            AllowedData(u1, false), AllowedData(
                                u2, true, teammate = true
                            ), AllowedData(u3, false)
                        )
                    )
                )
                defaultLeague.getCurrentMention() shouldBe "**Flo**, <@${u2}>"
            }
        }
    }
})

private data class GetCurrentMentionData(val current: Int, val allowed: MutableSet<AllowedData>)
