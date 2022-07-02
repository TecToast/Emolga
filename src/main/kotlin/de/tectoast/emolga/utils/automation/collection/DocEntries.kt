package de.tectoast.emolga.utils.automation.collection

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.DocEntry.*
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import de.tectoast.jsolf.JSONObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import java.util.function.Supplier

object DocEntries {
    @JvmField
    val UPL = DocEntry.create {
        leagueFunction =
            LeagueFunction { _: Long?, _: Long? -> Command.emolgaJSON.getJSONObject("drafts").getJSONObject("UPL") }
        killProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                gameday + 7,
                plindex * 12 + 2 + monindex
            )
        }
        deathProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                gameday + 23,
                plindex * 12 + 2 + monindex
            )
        }
        useProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                gameday + 15,
                plindex * 12 + 2 + monindex
            )
        }
        winProcessor = ResultStatProcessor { plindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                "AF",
                plindex * 12 + 1 + gameday
            )
        }
        looseProcessor = ResultStatProcessor { plindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                "AG",
                plindex * 12 + 1 + gameday
            )
        }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String? ->
                b.addRow(
                    "Spielplan!%s%d".formatted(
                        Command.getAsXCoord(gdi / 4 * 6 + 4), gdi % 4 * 6 + 6 + index
                    ), listOf<Any>(
                        numberOne, "=HYPERLINK(\"%s\"; \":\")".formatted(url), numberTwo
                    )
                )
            }
        setStatIfEmpty = false
        sorterData = SorterData("Tabelle!B2:H9", "Tabelle!B2:H9", false, null, 1, 4, 2)
    }

    @JvmField
    val PRISMA = DocEntry.create {
        leagueFunction = LeagueFunction { x: Long?, y: Long? ->
            Command.emolgaJSON.getJSONObject("drafts").getJSONObject("Prisma")
        }
        killProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                plindex * 11 + 2 + monindex
            )
        }
        deathProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 14,
                plindex * 11 + 2 + monindex
            )
        }
        winProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int -> StatLocation("Data", "W", plindex * 11 + 1 + gameday) }
        looseProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int -> StatLocation("Data", "X", plindex * 11 + 1 + gameday) }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String? ->
                b.addSingle(
                    "Spielplan!%s%d".formatted(
                        Command.getAsXCoord((if (gdi == 6) 1 else gdi % 3) * 3 + 3),
                        gdi / 3 * 5 + 4 + index
                    ), "=HYPERLINK(\"%s\"; \"%d:%d\")".formatted(url, numberOne, numberTwo)
                )
            }
        setStatIfEmpty = false
        sorterData = SorterData(
            "Tabelle!B2:I9", "Tabelle!B2:I9",
            true, { s: String -> s.substring("=Data!W".length).toInt() / 11 - 1 }, 1, -1, 7
        )
    }

    @JvmField
    val NDS = DocEntry.create {
        leagueFunction =
            LeagueFunction { x: Long?, y: Long? -> Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS") }
        tableMapper = Function { nds: JSONObject ->
            nds.getStringList("table").stream()
                .map { s: String? -> Command.reverseGet(nds.getJSONObject("teamnames"), (s)!!) }
                .map { s: String? -> s!!.toLong() }.toList()
        }
        killProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                plindex * 17 + 2 + monindex
            )
        }
        deathProcessor = StatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 18,
                plindex * 17 + 2 + monindex
            )
        }
        winProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int -> StatLocation("Data", gameday + 6, plindex * 17 + 18) }
        looseProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int -> StatLocation("Data", gameday + 18, plindex * 17 + 18) }
        resultCreator = BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, _: Int, _: Int, url: String? ->
            b.addSingle(
                "Spielplan HR!%s%d".formatted(
                    Command.getAsXCoord(gdi * 9 + 5), index * 10 + 4
                ),
                "=HYPERLINK(\"%s\"; \"Link\")".formatted(url)
            )
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            listOf("Tabelle HR!C3:K8", "Tabelle HR!C12:K17"), listOf("Tabelle HR!D3:K8", "Tabelle HR!D12:K17"),
            true, { s: String -> s.substring("=Data!F$".length).toInt() / 17 - 1 }, 1, -1, 7
        )
    }

    @JvmField
    val BSL = DocEntry.create {
        leagueFunction =
            LeagueFunction { x: Long?, y: Long? -> Command.emolgaJSON.getJSONObject("drafts").getJSONObject("BSL") }
        onlyKilllist = Supplier {
            try {
                Files.readAllLines(Paths.get("bslkilllists.txt"))
            } catch (e: IOException) {
                throw e
            }
        }
        setStatIfEmpty = false
        useProcessor = StatProcessor { _: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                monindex + 2
            )
        }
        killProcessor = StatProcessor { _: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 18,
                monindex + 2
            )
        }
    }
}