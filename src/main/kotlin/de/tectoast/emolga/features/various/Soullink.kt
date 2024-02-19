package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import kotlin.math.max

object Soullink {
    enum class Status {
        Team, Box, RIP
    }

    private fun Arguments.location() = fromList("Location", "Die Location", { db.soullink.only().order })
    private fun Arguments.status() =
        enumBasic<Status>("Status", "Der Status")

    fun String.eachWordUpperCase() =
        split(" ").joinToString(" ") { it.firstOrNull()?.uppercaseChar()?.plus(it.drop(1)) ?: "" }

    object AddLocationCommand : CommandFeature<AddLocationCommand.Args>(
        ::Args, CommandSpec("addlocation", "Fügt eine neue Location hinzu", 695943416789598208L)
    ) {
        class Args : Arguments() {
            var location by string("Location", "Die Location")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val soullink = db.soullink.only()
            val order = soullink.order
            val location = e.location.eachWordUpperCase()
            if (location in order) {
                return reply("Die Location gibt es bereits!")
            }
            order.add(location)
            reply("Die Location `$location` wurde eingetragen!")
            soullink.save()
            updateSoullink()
        }
    }

    object AddPokemonCommand : CommandFeature<AddPokemonCommand.Args>(
        ::Args,
        CommandSpec("addpokemon", "Fügt ein Pokemon hinzu", 695943416789598208)
    ) {
        class Args : Arguments() {
            var location by location()
            var pokemon by draftPokemon("Pokemon", "Das Pokemon")
            var status by status().nullable()
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            val soullink = db.soullink.only()
            val order = soullink.order
            val pokemon = e.pokemon.official
            val location = e.location.eachWordUpperCase()
            if (!order.contains(location)) {
                return reply("Die Location gibt es nicht! Falls es eine neue Location ist, füge diese mit `/addlocation` hinzu.")
            }
            val o = soullink.mons.getOrPut(location) { mutableMapOf() }
            o[soullinkIds[user]!!] = pokemon
            e.status?.let { o["status"] = it.name }
            reply("\uD83D\uDC4D")
            soullink.save()
            updateSoullink()
        }
    }

    object StatusCommand : CommandFeature<StatusCommand.Args>(
        ::Args,
        CommandSpec("status", "Setzt den Status eines Encounters", 695943416789598208)
    ) {
        class Args : Arguments() {
            var location by location()
            var status by status()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val soullink = db.soullink.only()
            val location = e.location.eachWordUpperCase()
            if (location !in soullink.order) {
                return reply("Diese Location ist derzeit nicht im System!")
            }
            soullink.mons[location]!!["status"] = e.status.name
            reply("\uD83D\uDC4D")
            soullink.save()
            updateSoullink()
        }
    }

    object UpdateSoullinkCommand :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("updatesoullink", "Updated die Message", 695943416789598208)) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            updateSoullink()
            done(true)
        }
    }

    val soullinkIds = mapOf(
        448542640850599947 to "Pascal",
        726495601021157426 to "David",
        867869302808248341 to "Jesse",
        541214204926099477 to "Felix"
    )
    private val soullinkNames = listOf("Pascal", "David", "Jesse", "Felix")

    suspend fun updateSoullink() {
        jda.getTextChannelById(Constants.SOULLINK_TCID)!!
            .editMessageById(Constants.SOULLINK_MSGID, buildSoullink()).queue()
    }

    private fun soullinkCols(): List<String> {
        return listOf(*soullinkNames.toTypedArray(), "Fundort", "Status")
    }

    private suspend fun buildSoullink(): String {
        val statusOrder = listOf("Team", "Box", "RIP")
        val soullink = db.soullink.only()
        val mons = soullink.mons
        val order = soullink.order
        val maxlen = max(order.maxOfOrNull { it.length } ?: -1,
            max(mons.values.flatMap { o -> o.values }.maxOfOrNull { obj -> obj.length } ?: -1, 7)) + 1
        val b = StringBuilder("```")
        soullinkCols().map { ew(it, maxlen) }.forEach { b.append(it) }
        b.append("\n")
        for (s in order.sortedBy {
            statusOrder.indexOf(
                //    mons.createOrGetJSON(it).optString("status", "Box")
                mons.getOrPut(it) { mutableMapOf() }["status"] ?: "Box"
            )
        }) {
            val o = mons.getOrPut(s) { mutableMapOf() }
            val status = o["status"] ?: "Box"
            b.append(soullinkCols().joinToString("") {
                ew(
                    when (it) {
                        "Fundort" -> s
                        "Status" -> status
                        else -> o[it] ?: ""
                    }, maxlen
                )
            }).append("\n")
        }
        return b.append("```").toString()
    }

    /**
     * Expand whitespaces
     *
     * @param str the string
     * @param len the length
     * @return the whitespaced string
     */
    private fun ew(str: String, len: Int): String {
        return str + " ".repeat(max(0, len - str.length))
    }

}
