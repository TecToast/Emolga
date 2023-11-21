package de.tectoast.emolga.commands.draft.during

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

object RandomPickCommand : TestableCommand<RandomPickCommandArgs>("randompick", "Well... nen Random-Pick halt") {
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

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = RandomPickCommandArgs(
        e.arguments.getText("tier"),
        e.arguments.getTranslation("type")
    )

    context (CommandData)
    override suspend fun exec(e: RandomPickCommandArgs) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        val mutex = locks.getOrPut(d.leaguename) { Mutex() }
        mutex.withLock {
            val tierlist = d.tierlist
            val tier = (tierlist.order.firstOrNull { e.tier.equals(it, ignoreCase = true) }
                ?: return reply("Das ist kein Tier!"))
                .takeIf {
                    tierRestrictions[gid]?.run { isEmpty() || contains(it) } != false
                }
                ?: return reply("In dieser Liga darf nur in folgenden Tiers gerandompickt werden: ${tierRestrictions[gid]?.joinToString()}")

            val list = tierlist.getByTier(tier)!!.shuffled()

            val typecheck: (suspend (String) -> Boolean) = e.type?.let { type ->
                { type.translation in db.pokedex.get(it.toSDName())!!.types }
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
            PickCommand.exec(PickCommandArgs(gambledPokemon, random = true))
        }
    }
}

class RandomPickCommandArgs(
    val tier: String,
    val type: Translation? = null
) : CommandArgs
