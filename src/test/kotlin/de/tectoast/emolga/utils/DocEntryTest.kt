package de.tectoast.emolga.utils

import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.records.Coord
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify

class DocEntryTest : FunSpec({
    test("SingleGame") {
        val monDataProviderCache = mockk<MonDataProviderCache>()
        val b = mockk<SimpleRequestBuilder>(relaxed = true)
        val fullGameData = FullGameData(
            listOf(2, 5),
            GamedayData(1, 3),
            listOf(
                ReplayData(
                    kd = listOf(
                        mapOf(
                            "Emolga" to KD(1, 0),
                            "Bisasam" to KD(1, 1)
                        ),
                        mapOf(
                            "Glumanda" to KD(0, 1),
                            "Schiggy" to KD(1, 1)
                        )
                    ),
                    winnerIndex = 0,
                    url = "https://example.com"
                )
            )
        )
        StatProcessorService.execute(
            monDataProviderCache,
            b,
            fullGameData,
            "",
            mapOf(
                2 to listOf(DraftPokemon("Emolga", "S"), DraftPokemon("Bisasam", "A")),
                5 to listOf(DraftPokemon("Glumanda", "B"), DraftPokemon("Schiggy", "C"))
            ),
            monsOrder = { list -> list.map { it.name } },
            statProcessors = listOf(StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, monIndex() + 5)) to DataTypeForMon.KILLS
            }, StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, 100 + monIndex() + 5)) to DataTypeForMon.KILLS
            }, StatProcessor {
                Coord("Data", gdi + 20, memIdx.y(10, monIndex() + 5)) to DataTypeForMon.DEATHS
            }, StatProcessor {
                Coord("Data", gdi + 4, memIdx.y(10, monIndex() + 5)) to DataTypeForMon.WINS
            }, StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, 0)) to DataTypeForMon.KILLS
            }),
            matchResultHandler = {}
        )
        verify(exactly = 1) {
            // Kills
            b.addSingle(Coord("Data", 3, 25), 1)
            b.addSingle(Coord("Data", 3, 26), 1)
            b.addSingle(Coord("Data", 3, 55), 0)
            b.addSingle(Coord("Data", 3, 56), 1)
            b.addSingle(Coord("Data", 3, 125), 1)
            b.addSingle(Coord("Data", 3, 126), 1)
            b.addSingle(Coord("Data", 3, 155), 0)
            b.addSingle(Coord("Data", 3, 156), 1)

            // Deaths
            b.addSingle(Coord("Data", 20, 25), 0)
            b.addSingle(Coord("Data", 20, 26), 1)
            b.addSingle(Coord("Data", 20, 55), 1)
            b.addSingle(Coord("Data", 20, 56), 1)

            // Wins
            b.addSingle(Coord("Data", 4, 25), 1)
            b.addSingle(Coord("Data", 4, 26), 1)
            b.addSingle(Coord("Data", 4, 55), 0)
            b.addSingle(Coord("Data", 4, 56), 0)

            // Kills aggregated
            b.addSingle(Coord("Data", 3, 20), 2)
            b.addSingle(Coord("Data", 3, 50), 1)
        }
    }
    test("Bo3") {
        val monDataProviderCache = mockk<MonDataProviderCache>()
        val b = mockk<SimpleRequestBuilder>(relaxed = true)
        val fullGameData = FullGameData(
            listOf(2, 5),
            GamedayData(1, 3),
            listOf(
                ReplayData(
                    kd = listOf(
                        mapOf(
                            "Emolga" to KD(1, 0),
                            "Bisasam" to KD(1, 1)
                        ),
                        mapOf(
                            "Glumanda" to KD(0, 1),
                            "Schiggy" to KD(1, 1)
                        )
                    ),
                    winnerIndex = 0,
                    url = "https://example.com"
                ),
                ReplayData(
                    kd = listOf(
                        mapOf(
                            "Emolga" to KD(0, 1),
                            "Bisasam" to KD(0, 1)
                        ),
                        mapOf(
                            "Glumanda" to KD(1, 0),
                            "Schiggy" to KD(1, 0)
                        )
                    ),
                    winnerIndex = 1,
                    url = "https://example.com"
                )
            )
        )
        StatProcessorService.execute(
            monDataProviderCache,
            b,
            fullGameData,
            "",
            mapOf(
                2 to listOf(DraftPokemon("Emolga", "S"), DraftPokemon("Bisasam", "A")),
                5 to listOf(DraftPokemon("Glumanda", "B"), DraftPokemon("Schiggy", "C"))
            ),
            monsOrder = { list -> list.map { it.name } },
            statProcessors = listOf(StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, monIndex() + 5)) to DataTypeForMon.KILLS
            }),
            matchResultHandler = {}
        )
        verify(exactly = 1) {
            // Kills
            b.addSingle(Coord("Data", 3, 25), 1 + 0)
            b.addSingle(Coord("Data", 3, 26), 1 + 0)
            b.addSingle(Coord("Data", 3, 55), 0 + 1)
            b.addSingle(Coord("Data", 3, 56), 1 + 1)
        }
    }
    test("MonName") {
        val monDataProviderCache = mockk<MonDataProviderCache>()
        coEvery { monDataProviderCache.getTLName("Emolga") } returns "BestPokemon"
        coEvery { monDataProviderCache.getTLName("Bisasam") } returns "Bisasam"
        coEvery { monDataProviderCache.getTLName("Glumanda") } returns "Glumanda"
        coEvery { monDataProviderCache.getTLName("Schiggy") } returns "Schiggy"
        val b = mockk<SimpleRequestBuilder>(relaxed = true)
        val fullGameData = FullGameData(
            listOf(2, 5),
            GamedayData(1, 3),
            listOf(
                ReplayData(
                    kd = listOf(
                        mapOf(
                            "Emolga" to KD(1, 0),
                            "Bisasam" to KD(1, 1)
                        ),
                        mapOf(
                            "Glumanda" to KD(0, 1),
                            "Schiggy" to KD(1, 1)
                        )
                    ),
                    winnerIndex = 0,
                    url = "https://example.com"
                )
            )
        )
        StatProcessorService.execute(
            monDataProviderCache,
            b,
            fullGameData,
            "",
            mapOf(
                2 to listOf(DraftPokemon("Emolga", "S"), DraftPokemon("Bisasam", "A")),
                5 to listOf(DraftPokemon("Glumanda", "B"), DraftPokemon("Schiggy", "C"))
            ),
            monsOrder = { list -> list.map { it.name } },
            statProcessors = listOf(StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, monIndex() + 5)) to DataTypeForMon.MONNAME
            }, StatProcessor {
                Coord("Data", gdi + 3, memIdx.y(10, monIndex() + 8)) to DataTypeForMon.KILLS
            }),
            matchResultHandler = {}
        )
        verify(exactly = 1) {
            // MonName
            b.addSingle(Coord("Data", 3, 25), "BestPokemon")
            b.addSingle(Coord("Data", 3, 26), "Bisasam")
            b.addSingle(Coord("Data", 3, 55), "Glumanda")
            b.addSingle(Coord("Data", 3, 56), "Schiggy")

            // Kills
            b.addSingle(Coord("Data", 3, 28), 1)
            b.addSingle(Coord("Data", 3, 29), 1)
            b.addSingle(Coord("Data", 3, 58), 0)
            b.addSingle(Coord("Data", 3, 59), 1)

        }
    }

})