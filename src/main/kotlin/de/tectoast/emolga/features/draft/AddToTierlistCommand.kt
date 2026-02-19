package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.dbAsync
import de.tectoast.emolga.database.exposed.DraftAdminsDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.NameConventionsDB.allNameConventions
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.during.generic.K18n_NoTierlist
import de.tectoast.emolga.features.draft.during.generic.K18n_TierNotFound
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.mdb
import mu.KotlinLogging
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.litote.kmongo.eq
import java.sql.SQLIntegrityConstraintViolationException

object AddToTierlistCommand : CommandFeature<AddToTierlistCommand.Args>(
    ::Args,
    CommandSpec("addtotierlist", K18n_AddToTierlist.Help)
) {
    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var mon by draftPokemon("Mon", K18n_AddToTierlist.ArgPokemon) { s, _ ->
            allNameConventions().filterStartsWithIgnoreCase(s).takeIf { it.size <= 25 }?.sorted()
        }
        var tier by string("Tier", K18n_AddToTierlist.ArgTier) {
            slashCommand { s, event ->
                Tierlist[event.guild!!.idLong]?.withTL { it.getTiers() }?.filter { it.startsWith(s) }
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
        val tierlist = Tierlist[id] ?: return iData.reply(K18n_NoTierlist)
        val mon = e.mon.tlName
        val allTiers = tierlist.withTL { it.getTiers() }
        val tier = e.tier ?: allTiers.last()
        if (tier !in allTiers) {
            return iData.reply(K18n_TierNotFound(tier))
        }
        try {
            tierlist.addPokemon(mon, tier)
        } catch (ex: ExposedSQLException) {
            if (ex.cause is SQLIntegrityConstraintViolationException) {
                return iData.reply(K18n_AddToTierlist.PokemonAlreadyInTierlist(mon))
            }
            throw ex
        }
        iData.reply(K18n_AddToTierlist.Success(mon, tier))
        val data = AddToTierlistData(mon, tier, tierlist, id).apply { addToTierlistAutocompletion() }
        val leagues = mdb.league.find(League::guild eq id).toList()
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

    val pkmn = dbAsync { mdb.getDataObject(mon, gid) }
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
