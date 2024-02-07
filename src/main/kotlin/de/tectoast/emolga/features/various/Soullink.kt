package de.tectoast.emolga.features.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only

object Soullink {
    enum class Status {
        Team, Box, RIP
    }

    private fun Arguments.location() = fromList("Location", "Die Location", { db.soullink.only().order })
    private fun Arguments.status() =
        enumBasic<Status>("Status", "Der Status")

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
            val location = Command.eachWordUpperCase(e.location)
            if (location in order) {
                return reply("Die Location gibt es bereits!")
            }
            order.add(location)
            reply("Die Location `$location` wurde eingetragen!")
            soullink.save()
            Command.updateSoullink()
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
            val location = Command.eachWordUpperCase(e.location)
            if (!order.contains(location)) {
                return reply("Die Location gibt es nicht! Falls es eine neue Location ist, füge diese mit `/addlocation` hinzu.")
            }
            val o = soullink.mons.getOrPut(location) { mutableMapOf() }
            o[Command.soullinkIds[user]!!] = pokemon
            e.status?.let { o["status"] = it.name }
            reply("\uD83D\uDC4D")
            soullink.save()
            Command.updateSoullink()
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
            val location = Command.eachWordUpperCase(e.location)
            if (location !in soullink.order) {
                return reply("Diese Location ist derzeit nicht im System!")
            }
            soullink.mons[location]!!["status"] = e.status.name
            reply("\uD83D\uDC4D")
            soullink.save()
            Command.updateSoullink()
        }
    }

    object UpdateSoullinkCommand :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("updatesoullink", "Updated die Message", 695943416789598208)) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            Command.updateSoullink()
            done(true)
        }
    }


}
