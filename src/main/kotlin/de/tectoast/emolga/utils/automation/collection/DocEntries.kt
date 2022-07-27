package de.tectoast.emolga.utils.automation.collection

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.emolgaJSON
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.DocEntry.*
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import de.tectoast.jsolf.JSONObject
import java.util.function.Function

object DocEntries {
    @JvmField
    val UPL = DocEntry.create {
        leagueFunction =
            LeagueFunction { _: Long, _: Long -> emolgaJSON.getJSONObject("drafts").getJSONObject("UPL") }
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                gameday + 7,
                plindex * 12 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Stats",
                gameday + 23,
                plindex * 12 + 2 + monindex
            )
        }
        useProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
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
                    "Spielplan!${getAsXCoord(gdi / 4 * 6 + 4)}${gdi % 4 * 6 + 6 + index}", listOf<Any>(
                        numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo
                    )
                )
            }
        setStatIfEmpty = false
        sorterData = SorterData("Tabelle!B2:H9", false, null, 1, 4, 2)
    }

    @JvmField
    val PRISMA = DocEntry.create {
        leagueFunction = LeagueFunction { _: Long, _: Long ->
            emolgaJSON.getJSONObject("drafts").getJSONObject("Prisma")
        }
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                plindex * 11 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
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
                    "Spielplan!${getAsXCoord((if (gdi == 6) 1 else gdi % 3) * 3 + 3)}${gdi / 3 * 5 + 4 + index}",
                    "=HYPERLINK(\"$url\"; \"$numberOne:$numberTwo\")"
                )
            }
        setStatIfEmpty = false
        sorterData = SorterData(
            "Tabelle!B2:I9",
            true, { s: String -> s.substring("=Data!W".length).toInt() / 11 - 1 }, 1, -1, 7
        )
    }

    @JvmField
    val NDS = DocEntry.create {
        leagueFunction =
            LeagueFunction { _: Long, _: Long -> emolgaJSON.getJSONObject("drafts").getJSONObject("NDS") }
        tableMapper = Function { nds: JSONObject ->
            nds.getStringList("table").stream()
                .map { s: String? -> Command.reverseGet(nds.getJSONObject("teamnames"), (s)!!) }
                .map { s: String? -> s!!.toLong() }.toList()
        }
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                plindex * 17 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
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
                "Spielplan HR!${getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}",
                "=HYPERLINK(\"$url\"; \"Link\")"
            )
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            listOf("Tabelle RR!C3:K8", "Tabelle RR!C12:K17"),
            true, { it.substring("=Data!F$".length).toInt() / 17 - 1 }, 2, 8, -1
        )
    }


    @JvmField
    val GDL = DocEntry.create {
        leagueFunction = LeagueFunction { uid1: Long, uid2: Long ->
            val drafts = emolgaJSON.getJSONObject("drafts")
            if (drafts.getJSONObject("GDL1").getLongList("table").containsAll(listOf(uid1, uid2))) {
                drafts.getJSONObject("GDL1")
            } else drafts.getJSONObject("GDL2")
        }
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 15 + 5 + monindex)
        }
        deathProcessor = CombinedStatProcessor { plindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 15 + 16)
        }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String ->
                b.addRow(
                    "Spielplan!${getAsXCoord(gdi % 3 * 6 + 3)}${gdi / 3 * 9 + 4 + index}",
                    listOf(numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo)
                )
            }
    }
}