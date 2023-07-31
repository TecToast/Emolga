package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.litote.kmongo.eq
import java.sql.SQLIntegrityConstraintViolationException

class AddToTierlistCommand :
    Command("addtotierlist", "Fügt ein Mon in die Tierliste ein", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("mon", "Mon", "Das Mon", ArgumentManagerTemplate.draftPokemon { s, _ ->
                allNameConventions.filterStartsWithIgnoreCase(s).takeIf { it.size <= 25 }?.sorted()
            }, false, "Das ist kein Pokemon!")
            .add(
                "tier",
                "Tier",
                "Das Tier, sonst das unterste",
                ArgumentManagerTemplate.Text.withAutocomplete { s, event ->
                    Tierlist[event.guild!!.idLong]?.prices?.keys?.filter { it.startsWith(s) }
                },
                true
            )
            .setExample("/addtotierlist Chimstix")
            .build()
        setCustomPermissions(PermissionPreset.fromRole(702233714360582154).or(PermissionPreset.ADMIN))
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val id = League.onlyChannel(e.channel.idLong)?.guild ?: e.guild.idLong
        val tierlist = Tierlist[id] ?: return e.reply("Es gibt keine Tierlist für diesen Server!")
        val mon = e.arguments.getDraftName("mon").tlName
        val tier = e.arguments.getNullable<String>("tier") ?: tierlist.prices.keys.last()
        if (tier !in tierlist.prices) {
            e.reply("Das Tier $tier existiert nicht!")
            return
        }
        try {
            tierlist.addPokemon(mon, tier)
        } catch (ex: ExposedSQLException) {
            if (ex.cause is SQLIntegrityConstraintViolationException) {
                e.reply("Das Pokemon $mon existiert bereits!")
                return
            }
            e.reply("Es ist ein unbekannter Fehler aufgetreten!")
            ex.printStackTrace()
            return
        }
        e.reply("`$mon` ist nun im $tier-Tier!")
        val data = AddToTierlistData(mon, tier, tierlist, id)
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

data class AddToTierlistData(val mon: String, val tier: String, val tierlist: Tierlist, val gid: Long) {

    val pkmn by lazy { runBlocking { Command.getDataObject(mon, gid) } }
    val englishTLName by lazy {
        runBlocking {
            NameConventionsDB.getDiscordTranslation(
                mon,
                gid,
                english = true
            )!!.tlName
        }
    }
    val index by lazy { tierlist.monCount - 1 }

    init {
        tierlist.addedViaCommand += mon
        tierlist.addedViaCommand += englishTLName
    }
}
