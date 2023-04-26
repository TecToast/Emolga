package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.Emolga
import org.jetbrains.exposed.exceptions.ExposedSQLException
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
        setCustomPermissions(PermissionPreset.fromRole(702233714360582154))
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val id = e.guild.idLong
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

        val leagues = Emolga.get.drafts.values.filter { it.guild == id }
        if (leagues.isNotEmpty()) {
            val data = AddToTierlistData(mon, tier, tierlist.monCount - 1, id)
            leagues.forEach {
                with(it) {
                    data.addMonToTierlist()
                }
            }
        }
    }
}

data class AddToTierlistData(val mon: String, val tier: String, val index: Int, val gid: Long) {
    val pkmn by lazy { Command.getDataObject(mon, gid) }
    val englishTLName by lazy { NameConventionsDB.getDiscordTranslation(mon, gid, english = true)!!.tlName }
}
