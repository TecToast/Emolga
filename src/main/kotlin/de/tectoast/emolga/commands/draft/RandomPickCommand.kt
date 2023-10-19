package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object RandomPickCommand : Command("randompick", "Well... nen Random-Pick halt", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("tier", "Tier", "Das Tier, in dem gepickt werden soll", ArgumentManagerTemplate.Text.any())
            .addEngl("type", "Typ", "Der Typ, von dem random gepickt werden soll", Translation.Type.TYPE, true)
            .setExample("/randompick A").build()
        slash(true, Constants.G.ASL, Constants.G.FLP, Constants.G.WFS)
    }


    private val tierRestrictions = mapOf(
        Constants.G.ASL to setOf("D")
    )
    private val locks = ConcurrentHashMap<String, Mutex>()


    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        val mutex = locks.getOrPut(d.leaguename) { Mutex() }
        mutex.withLock {
            val tierlist = d.tierlist
            val args = e.arguments
            val gid = e.guild.idLong
            val tier = (tierlist.order.firstOrNull { args.getText("tier").equals(it, ignoreCase = true) } ?: run {
                e.reply("Das ist kein Tier!")
                return
            }).takeIf { tierRestrictions[gid]?.run { isEmpty() || contains(it) } ?: true } ?: run {
                return e.reply("In dieser Liga darf nur in folgenden Tiers gerandompickt werden: ${tierRestrictions[gid]?.joinToString()}")
            }
            val list: MutableList<String> = tierlist.getByTier(tier)!!.toMutableList()
            list.shuffle()
            val typecheck: (suspend (String) -> Boolean)? = if (args.has("type")) {
                val type = args.getTranslation("type");
                { type.translation in db.pokedex.get(it.toSDName())!!.types }
            } else null
            e.arguments.map.apply {
                put("pokemon", (list.firstNotNullOfOrNull { str: String ->
                    val draftName = NameConventionsDB.getDiscordTranslation(
                        str, d.guild, tierlist.isEnglish
                    )!!
                    draftName.takeIf {
                        !d.isPicked(
                            draftName.official,
                            tier
                        ) && typecheck?.invoke(
                            NameConventionsDB.getDiscordTranslation(
                                str, d.guild, true
                            )!!.official
                        ) != false
                    }
                } ?: return e.reply("In diesem Tier gibt es kein Pokemon mit dem angegebenen Typen mehr!")))
            }
            PickCommand.exec(e, true)
        }
    }
}
