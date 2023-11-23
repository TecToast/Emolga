package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.*
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.HENNY
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

private data class GetCurrentMentionData(val current: Long, val allowed: MutableSet<AllowedData>)
class LeagueTest : FunSpec({

    lateinit var defaultLeague: League
    val logger = KotlinLogging.logger {}

    beforeTest { defaultLeague = Default() }

    context("DirectChecks") {
        context("getCurrentMention") {
            beforeTest {
                logger.info { "setting names" }
                defaultLeague.names[FLOID] = "Flo"
                defaultLeague.names[HENNY] = "Henny"
                defaultLeague.names[723829878755164202] = "Emolga"
            }
            fun apply(data: GetCurrentMentionData) {
                defaultLeague.current = data.current
                defaultLeague.allowed.clear()
                defaultLeague.allowed[data.current] = data.allowed
            }
            test("only self mention") {
                apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, false))))
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>"
            }
            test("both mention") {
                apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, true), AllowedData(HENNY, true))))
                defaultLeague.getCurrentMention() shouldBe "<@$FLOID>, ||<@$HENNY>||"
            }
            test("only other mention") {
                apply(GetCurrentMentionData(FLOID, mutableSetOf(AllowedData(FLOID, false), AllowedData(HENNY, true))))
                defaultLeague.getCurrentMention() shouldBe "**Flo**, ||<@$HENNY>||"
            }
            test("with only teammate") {
                apply(
                    GetCurrentMentionData(
                        FLOID, mutableSetOf(
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
                        FLOID, mutableSetOf(
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
                        FLOID, mutableSetOf(
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
                        FLOID, mutableSetOf(
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
            defaultLeague.names shouldBe mutableMapOf()
        }

        test("AppendedEmbed") {
            val url = "https://replay.pokemonshowdown.com/gen9nationaldexag-1984877421"
            val data = Analysis.analyse(url)
            val uids = listOf(324265924905402370, 207211269911085056)
            val league = db.league("ASLS13L0")
            defaultChannel.send(
                content = url, embeds = league.appendedEmbed(
                    data,
                    LeagueResult(league, uids, otherForms = emptyMap()),
                    league.getGameplayData(uids[0], uids[1], data.game)
                ).build().into()
            ).queue()
        }
    }
    context("Automation Checks") {
        test("Test") {
            enableReplyRedirect()
            createDraft(
                name = "Test",
                playerCount = 4,
                rounds = 1,
                hardcodedUserIds = mapOf(0 to FLOID),
                originalorder = mapOf(1 to listOf(0, 1, 2, 3))
            )
            startDraft("Test")
            movePick()
            randomPick("S")
            pick("Glastrier")
            pick("Emolga")
            pick("Heatran")
            db.league("TESTTest").isRunning shouldBe false
            keepAlive()
        }
    }
})
