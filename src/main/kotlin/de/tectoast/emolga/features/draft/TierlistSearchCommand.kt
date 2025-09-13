package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.db

object TierlistSearchCommand : CommandFeature<TierlistSearchCommand.Args>(
    ::Args,
    CommandSpec("tierlistsearch", "Zeigt dir alle Pokemon in einem Tier mit einem bestimmten Typ")
) {
    class Args : Arguments() {
        var tier by string("Tier", "Das Tier, in dem du suchen möchtest")
        var type by pokemontype("Typ", "Der Typ, den du suchen möchtest")
    }

    private val dataCache = mutableMapOf<Long, MutableMap<String, List<String>>>()

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val tierlist = Tierlist[iData.gid] ?: run {
            return iData.reply("Es wurde keine Tierliste für diesen Server hinterlegt!")
        }
        val tier = e.tier
        val mons = tierlist.getByTier(tier) ?: run {
            iData.reply("Das Tier existiert auf diesem Server nicht!")
            return
        }
        iData.deferReply(ephemeral = true)
        val searchType = e.type
        val searchTypeEnglish = searchType.english
        val filteredList = mons.filter {
            searchTypeEnglish in dataCache.getOrPut(iData.gid) { mutableMapOf() }
                .getOrPut(it) { db.getDataObject(it, iData.gid).types }
        }
        iData.reply(
            "All diese Mons aus dem ${tier}-Tier besitzen den Typen ${searchType.german}:\n${
                filteredList.joinToString(
                    "\n"
                )
            }", ephemeral = true
        )
    }
}
