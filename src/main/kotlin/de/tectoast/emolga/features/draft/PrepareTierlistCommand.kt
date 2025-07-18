package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.DocRange

object PrepareTierlistCommand : CommandFeature<PrepareTierlistCommand.Args>(
    ::Args,
    CommandSpec(
        "preparetierlist",
        "Richtet die Tierliste ein",
    )
) {
    init {
        restrict(admin)
    }

    class Args : Arguments() {
        var gid by long("Guild-ID", "Die ID der Guild")
        var docurl by string("Doc-URL", "Die URL des Dokuments, in dem die Namen stehen")
        var tierlistsheet by string("Tierlist-Sheet", "Der Name des Tierlist-Sheets")
        var ranges by list("Bereich %s", "Der %s. Bereich", 10, 1)
        var complexSign by string("Komplexsymbol", "Das Symbol für Komplexbanns").nullable()
        var tlIdentifier by string("TL-Identifier", "Der Identifier für die Tierlist").nullable()
        var shiftMode by enumBasic<ShiftMode>("Shift-Mode", "Der Shift-Mode").nullable()
        var shiftData by string("Shift-Data", "Die Shift-Data").nullable()
        var dataMapper by enumBasic<DataMapper>("DataMapper", "Der potenzielle DataMapper").nullable()
        var language by enumBasic<Language>("Sprache", "Die Sprache der Tierliste") {
            default = Language.GERMAN
        }
    }

    enum class ShiftMode {
        N {
            val regex = Regex("(.*?)\\s\\S+\\s?->\\s?(\\S+)")
            override fun parseMons(str: String): List<DraftPokemon> {
                return regex.findAll(str).map {
                    val (mon, tier) = it.destructured
                    DraftPokemon(mon.trim(), tier.trim())
                }.toList()
            }
        },
        H {
            override fun parseMons(str: String): List<DraftPokemon> {
                val split = str.split(Regex("\\s+"))
                return buildList {
                    var index = 0
                    var currentTier = ""
                    while (index in split.indices) {
                        val curr = split[index]
                        if (curr.endsWith(":")) {
                            currentTier = curr.replace(":", "").trim()
                            index++
                            continue
                        }
                        this += DraftPokemon(curr, currentTier)
                        index += 2
                    }
                }
            }
        };

        abstract fun parseMons(str: String): List<DraftPokemon>
    }

    enum class DataMapper {
        MDL {
            val data = OneTimeCache {
                val yCoords = listOf(3, 59, 105, 155, 214)
                val ranges = buildList {
                    for (i in 0..<18) {
                        val y = yCoords[i / 4]
                        add(
                            i.CoordXMod("Tierlist", 4, 6, 8, 56, 3).setY(y)
                                .spreadTo(2, yCoords.getOrNull(i / 4 + 1)?.minus(y + 1) ?: 60)
                        )
                    }
                }
                val get = Google.batchGet("1iPnZKaAUW9xn5cgL-ME45a1P_FZksB_VJOJi90_Vw_8", ranges, false)
                buildMap {
                    for (subList in get) {
                        val type = subList[0][1].toString()
                        subList.drop(2).forEach {
                            put(it[1].toString(), ExternalTierlistData(it[0].toString(), type))
                        }
                    }
                }
            }

            override suspend fun dataOf(mon: String): ExternalTierlistData {
                return data()[mon] ?: error("No Data found for $mon")
            }
        },

        UDT {
            val data = OneTimeCache {
                val get = Google.batchGet(
                    "1N0RjZY-G-3l15vJLbSN_5QOtN9_OwskOYB-5rYsuyg0",
                    (0..<5).map { it.CoordXMod("Tierliste", 5, 7, 3, 0, 6).spreadTo(x = 2, y = 140) }, false
                )
                buildMap {
                    for (row in get.flatten()) {
                        put(
                            row[0].toString(),
                            ExternalTierlistData(points = row[1].let {
                                if (it == "BANN") Int.MAX_VALUE else it.toString().toInt()
                            })
                        )
                    }
                }
            }

            override suspend fun dataOf(mon: String): ExternalTierlistData {
                return data()[mon] ?: error("No Data found for $mon")
            }
        };

        abstract suspend fun dataOf(mon: String): ExternalTierlistData
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val sid = e.docurl.substringAfter("d/").substringBefore("/")
        val tierlistsheet = e.tierlistsheet
        iData.deferReply()
        val tierlistcols = mutableListOf<List<String>>()
        val shiftedMons = e.shiftData?.let {
            e.shiftMode!!.parseMons(it)
        }
        val complexSign = e.complexSign
        val ranges = e.ranges.flatMap { it.split(";") }
        try {
            val (_, _, yStart, _, yEnd) = DocRange[ranges.first()]
            TierlistBuilderConfigurator(
                userId = iData.user,
                channelId = iData.tc,
                guildId = e.gid,
                mons =
                    (Google.batchGet(
                        sid,
                        ranges.map {
                            if (it.contains(":")) return@map "$tierlistsheet!$it"
                            "$tierlistsheet!$it$yStart:$it$yEnd"
                        },
                        false,
                        "COLUMNS"
                    )
                        .mapNotNull { col -> col.flatten().mapNotNull { it.toString().prepareForTL(complexSign) } }
                        .also { tierlistcols += it }
                        .flatten().ensureNoDuplicates() + shiftedMons?.map { it.name }.orEmpty()).distinct(),
                tierlistcols = tierlistcols,
                shiftedMons = shiftedMons,
                tierMapper = e.dataMapper?.let { mapper -> { mapper.dataOf(it) } },
                tlIdentifier = e.tlIdentifier,
                language = e.language
            )
        } catch (ex: DuplicatesFoundException) {
            iData.reply(
                "Es wurden Pokemon doppelt in der Tierliste gefunden! Bitte überprüfe die folgenden Pokemon: ${
                    ex.duplicates.joinToString(
                        ", "
                    )
                }"
            )
        }
    }
}

private val complexSigns = setOf("*", "^")
private fun String.prepareForTL(complexSign: String?): String? {
    if (toIntOrNull() != null) return null
    var x = this
    complexSigns.forEach { x = x.substringBefore(it) }
    complexSign?.let { x = x.substringBefore(it) }
    return x.trim().takeUnless { it.isBlank() }
}

fun List<String>.ensureNoDuplicates(): List<String> {
    return groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList().takeIf { it.isNotEmpty() }?.let {
        throw DuplicatesFoundException(it)
    } ?: this
}

class DuplicatesFoundException(val duplicates: List<String>) : Exception()

data class ExternalTierlistData(val tier: String? = null, val type: String? = null, val points: Int? = null)
