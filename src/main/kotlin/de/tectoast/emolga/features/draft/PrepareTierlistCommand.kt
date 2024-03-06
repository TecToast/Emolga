package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.DraftPokemon

object PrepareTierlistCommand : CommandFeature<PrepareTierlistCommand.Args>(
    ::Args,
    CommandSpec(
        "preparetierlist",
        "Richtet die Tierliste ein",
        *TierlistBuilderConfigurator.enabledGuilds.toLongArray()
    )
) {
    init {
        restrict(admin)
    }

    class Args : Arguments() {
        var docurl by string("Doc-URL", "Die URL des Dokuments, in dem die Namen stehen")
        var tierlistsheet by string("Tierlist-Sheet", "Der Name des Tierlist-Sheets")
        var ranges by list("Bereich %s", "Der %s. Bereich", 10, 1)
        var shiftMode by enumBasic<ShiftMode>("Shift-Mode", "Der Shift-Mode").nullable()
        var shiftData by string("Shift-Data", "Die Shift-Data").nullable()
    }

    enum class ShiftMode {
        N {
            override fun parseMons(str: String): List<DraftPokemon> {
                val split = str.split(" ")
                return buildList {
                    for (i in split.indices step 2) {
                        this += DraftPokemon(split[i], split[i + 1])
                    }
                }
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


    context(InteractionData)
    override suspend fun exec(e: Args) {
        val sid = e.docurl.substringAfter("d/").substringBefore("/")
        val tierlistsheet = e.tierlistsheet
        deferReply()
        val tierlistcols = mutableListOf<List<String>>()
        val shiftedMons = e.shiftData?.let {
            e.shiftMode!!.parseMons(it)
        }
        try {
            TierlistBuilderConfigurator(
                userId = user,
                channelId = tc,
                guildId = PrivateCommands.guildForMyStuff?.takeUnless { isNotFlo } ?: gid,
                mons =
                (Google.batchGet(
                    sid,
                    e.ranges.map { "$tierlistsheet!$it" },
                    false,
                    "COLUMNS"
                )
                    .map { col -> col.flatten().mapNotNull { it.toString().prepareForTL() } }
                    .also { tierlistcols += it }
                    .flatten().ensureNoDuplicates() + shiftedMons?.map { it.name }.orEmpty()).distinct(),
                tierlistcols = tierlistcols,
                shiftedMons = shiftedMons
            )
        } catch (ex: DuplicatesFoundException) {
            reply(
                "Es wurden Pokemon doppelt in der Tierliste gefunden! Bitte überprüfe die folgenden Pokemon: ${
                    ex.duplicates.joinToString(
                        ", "
                    )
                }"
            )
        }
    }
}

private val complexSigns = setOf("*", "^", "(")
private fun String.prepareForTL(): String? {
    if (toIntOrNull() != null) return null
    var x = this
    complexSigns.forEach { x = x.substringBefore(it) }
    return x.trim().takeUnless { it.isBlank() }
}

fun List<String>.ensureNoDuplicates(): List<String> {
    return groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList().takeIf { it.isNotEmpty() }?.let {
        throw DuplicatesFoundException(it)
    } ?: this
}

class DuplicatesFoundException(val duplicates: List<String>) : Exception()
