package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import dev.minn.jda.ktx.messages.send


object TierlistSearchCommand :
    Command("tierlistsearch", "Zeigt dir alle Pokemon in einem Tier mit einem bestimmten Typ", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("tier", "Tier", "Das Tier, in dem du suchen möchtest", ArgumentManagerTemplate.Text.any())
            add("type", "Typ", "Der Typ, den du suchen möchtest", Translation.Type.TYPE, language = Language.ENGLISH)
            example = "!tierlistsearch A Water"
        }
        slash(true, Constants.G.ASL)
    }

    val dataCache = mutableMapOf<Long, MutableMap<String, List<String>>>()

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val gid = e.guild.idLong
        val tierlist = Tierlist[gid] ?: run {
            e.reply("Es wurde keine Tierliste für diesen Server hinterlegt!")
            return
        }
        val tier = args.getText("tier")
        val mons = tierlist.getByTier(tier) ?: run {
            e.reply("Das Tier existiert auf diesem Server nicht!")
            return
        }
        e.deferReply(ephermal = true)
        val searchType = args.getTranslation("type")
        val searchTypeEnglish = searchType.translation
        val filteredList = mons.filter {
            searchTypeEnglish in dataCache.getOrPut(gid) { mutableMapOf() }
                .getOrPut(it) { getDataObject(it, gid).types }
        }
        e.hook.send(
            "All diese Mons aus dem ${tier}-Tier besitzen den Typen ${searchType.otherLang}:\n${
                filteredList.joinToString(
                    "\n"
                )
            }", ephemeral = true
        ).queue()
    }

}
