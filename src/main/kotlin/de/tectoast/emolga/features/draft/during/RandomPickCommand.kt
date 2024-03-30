package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object RandomPickCommand : CommandFeature<RandomPickCommand.Args>(
    ::Args, CommandSpec("randompick", "Well... nen Random-Pick halt", Constants.G.ASL, Constants.G.FLP, Constants.G.WFS)
) {
    class Args : Arguments() {
        var tier by string("tier", "Das Tier, in dem gepickt werden soll").nullable()
        var type by pokemontype("type", "Der Typ, von dem random gepickt werden soll", true).nullable()
    }

    private val tierRestrictions = mapOf(
        Constants.G.ASL to setOf("D")
    )
    private val locks = ConcurrentHashMap<String, Mutex>()

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val d = League.byCommand()?.first ?: return reply(
            "Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true
        )
        val mutex = locks.getOrPut(d.leaguename) { Mutex() }
        mutex.withLock {
            val tierlist = d.tierlist
            val tier = e.tier?.let {
                (tierlist.order.firstOrNull { e.tier.equals(it, ignoreCase = true) }
                    ?: return reply("Das ist kein Tier!")).takeIf {
                    tierRestrictions[gid]?.run { isEmpty() || contains(it) } != false
                }
                    ?: return reply("In dieser Liga darf nur in folgenden Tiers gerandompickt werden: ${tierRestrictions[gid]?.joinToString()}")
            } ?: if (d.tierlist.mode.withTiers) d.getPossibleTiers()
                .filter { it.value > 0 }.keys.random() else d.tierlist.order.last()

            val list = tierlist.getByTier(tier)!!.shuffled()

            val typecheck: (suspend (String) -> Boolean) = e.type?.let { type ->
                { type in db.pokedex.get(it.toSDName())!!.types }
            } ?: { true }
            val gambledPokemon = (list.firstNotNullOfOrNull { str: String ->
                val draftName = NameConventionsDB.getDiscordTranslation(
                    str, d.guild, tierlist.isEnglish
                )!!
                draftName.takeIf {
                    !d.isPicked(
                        draftName.official, tier
                    ) && typecheck(
                        NameConventionsDB.getDiscordTranslation(
                            str, d.guild, true
                        )!!.official
                    )
                }
            } ?: return reply("In diesem Tier gibt es kein Pokemon mit dem angegebenen Typen mehr!"))
            PickCommand.exec(PickCommand.buildArgs {
                pokemon = gambledPokemon
                random = true
            })
        }
    }
}
