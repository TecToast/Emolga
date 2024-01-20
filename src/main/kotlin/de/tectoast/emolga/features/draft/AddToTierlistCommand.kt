package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.Command.Companion.allNameConventions
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.draft.AddToTierlistData
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.litote.kmongo.eq
import java.sql.SQLIntegrityConstraintViolationException

object AddToTierlistCommand : CommandFeature<AddToTierlistCommand.Args>(
    ::Args,
    CommandSpec("addtotierlist", "Fügt ein Mon in die Tierliste ein", *draftGuilds)
) {
    class Args : Arguments() {
        var mon by draftPokemon("Mon", "Das Mon") { s, _ ->
            allNameConventions.filterStartsWithIgnoreCase(s).takeIf { it.size <= 25 }?.sorted()
        }
        var tier by string("Tier", "Das Tier, sonst das unterste") {
            slashCommand { s, event ->
                Tierlist[event.guild!!.idLong]?.prices?.keys?.filter { it.startsWith(s) }
            }
        }.nullable()
    }

    override val check: suspend InteractionData.() -> Boolean = {
        roles(702233714360582154)() || admin(this)
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val id = League.onlyChannel(tc)?.guild ?: gid
        val tierlist = Tierlist[id] ?: return reply("Es gibt keine Tierlist für diesen Server!")
        val mon = e.mon.tlName
        val tier = e.tier ?: tierlist.prices.keys.last()
        if (tier !in tierlist.prices) {
            return reply("Das Tier $tier existiert nicht!")
        }
        try {
            tierlist.addPokemon(mon, tier)
        } catch (ex: ExposedSQLException) {
            if (ex.cause is SQLIntegrityConstraintViolationException) {
                return reply("Das Pokemon $mon existiert bereits!")
            }
            reply("Es ist ein unbekannter Fehler aufgetreten!")
            ex.printStackTrace()
            return
        }
        reply("`$mon` ist nun im $tier-Tier!")
        val data = AddToTierlistData(mon, tier, tierlist, id).apply { addToTierlistAutocompletion() }
        val leagues = db.drafts.find(League::guild eq id).toList()
        if (leagues.isNotEmpty()) {
            leagues.forEach {
                with(it) {
                    data.addMonToTierlist()
                }
            }
        }
    }
}
