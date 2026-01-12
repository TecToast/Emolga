package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.dbAsync
import de.tectoast.emolga.database.exposed.DraftAdminsDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.NameConventionsDB.allNameConventions
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.db
import mu.KotlinLogging
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.litote.kmongo.eq
import java.sql.SQLIntegrityConstraintViolationException

object AddToTierlistCommand : CommandFeature<AddToTierlistCommand.Args>(
    ::Args,
    CommandSpec("addtotierlist", "Fügt ein Mon in die Tierliste ein")
) {
    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var mon by draftPokemon("Mon", "Das Mon") { s, _ ->
            allNameConventions().filterStartsWithIgnoreCase(s).takeIf { it.size <= 25 }?.sorted()
        }
        var tier by string("Tier", "Das Tier, sonst das unterste") {
            slashCommand { s, event ->
                Tierlist[event.guild!!.idLong]?.prices?.keys?.filter { it.startsWith(s) }
            }
        }.nullable()
    }

    init {
        restrict { admin(this) || DraftAdminsDB.isAdmin(gid, member()) }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        // TODO: Add support for multiple tierlists via identifier
        val id = League.onlyChannel(iData.tc)?.guild ?: iData.gid
        val tierlist = Tierlist[id] ?: return iData.reply("Es gibt keine Tierlist für diesen Server!")
        val mon = e.mon.tlName
        val tier = e.tier ?: tierlist.prices.keys.last()
        if (tier !in tierlist.prices) {
            return iData.reply("Das Tier `$tier` existiert nicht!")
        }
        try {
            tierlist.addPokemon(mon, tier)
        } catch (ex: ExposedSQLException) {
            if (ex.cause is SQLIntegrityConstraintViolationException) {
                return iData.reply("Das Pokemon `$mon` existiert bereits!")
            }
            iData.reply("Es ist ein unbekannter Fehler aufgetreten!")
            logger.error("Error in AddToTierlistCommand", ex)
            return
        }
        iData.reply("`$mon` ist nun im $tier-Tier!")
        val data = AddToTierlistData(mon, tier, tierlist, id).apply { addToTierlistAutocompletion() }
        val leagues = db.league.find(League::guild eq id).toList()
        if (leagues.isNotEmpty()) {
            leagues.forEach {
                with(it) {
                    data.addMonToTierlist()
                }
            }
        }
    }
}

data class AddToTierlistData(val mon: String, val tier: String, val tierlist: Tierlist, val gid: Long) {

    val pkmn = dbAsync { db.getDataObject(mon, gid) }
    val englishTLName = dbAsync {
        NameConventionsDB.getDiscordTranslation(
            mon,
            gid,
            english = true
        )!!.tlName
    }

    val index = OneTimeCache { tierlist.getMonCount() - 1 }

    suspend fun addToTierlistAutocompletion() {
        tierlist.addedViaCommand += mon
        tierlist.addedViaCommand += englishTLName.await()
        TierlistBuilderConfigurator.checkTL(listOf(mon, englishTLName.await()), gid)
    }
}
