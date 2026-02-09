package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.during.generic.K18n_NoTierlist
import de.tectoast.emolga.features.draft.during.generic.K18n_TierNotFound
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.db

object TierlistSearchCommand : CommandFeature<TierlistSearchCommand.Args>(
    ::Args,
    CommandSpec("tierlistsearch", K18n_TierlistSearch.Help)
) {
    class Args : Arguments() {
        var tier by string("Tier", K18n_TierlistSearch.ArgTier)
        var type by pokemontype("Typ", K18n_TierlistSearch.ArgType)
    }

    private val dataCache = mutableMapOf<Long, MutableMap<String, List<String>>>()

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val tierlist = Tierlist[iData.gid] ?: run {
            return iData.reply(K18n_NoTierlist, ephemeral = true)
        }
        val tier = e.tier
        val mons = tierlist.getByTier(tier) ?: run {
            iData.reply(K18n_TierNotFound(tier), ephemeral = true)
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
            K18n_TierlistSearch.Success(tier, searchType.german, filteredList.joinToString("\n")), ephemeral = true
        )
    }
}
