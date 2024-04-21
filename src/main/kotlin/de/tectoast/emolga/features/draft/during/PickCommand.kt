package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.emolga.draft.*
import mu.KotlinLogging

object PickCommand :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", "Pickt ein Pokemon", *draftGuilds)) {
    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var pokemon by draftPokemon("pokemon", "Das Pokemon, das gepickt werden soll")
        var tier by string("tier", "Das Tier, in dem das Pokemon gepickt werden soll") {

            slashCommand { s, event ->
                League.onlyChannel(event.channel.idLong)?.getPossibleTiers(forAutocomplete = true)
                    ?.filter { it.value > 0 }?.map { it.key }?.filterStartsWithIgnoreCase(s)
            }
        }.nullable()
        var free by boolean("free", "Ob dieser Pick ein Freepick ist") {
            default = false
            slashCommand(guildChecker = {
                Tierlist[it]?.mode?.isTiersWithFree() ?: false
            })
        }
        var random by boolean("random", "RANDOMPICK (not visible)") {
            onlyInCode = true
            default = false
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            beforePick()?.let { return reply(it) }
            executeWithinLock(
                e.pokemon,
                e.tier,
                e.free,
                if (e.random) PickMessageType.RANDOM else PickMessageType.REGULAR
            )
        }
    }

    context(InteractionData)
    suspend fun League.executeWithinLock(
        pokemon: DraftName,
        tier: String?,
        free: Boolean,
        type: PickMessageType
    ) {
        val pickData = formalPick(pokemon, tier, free) as? PickData ?: return
        with(pickData) {
            type.reply()
        }
        insertIntoDoc(pickData)
        if (type != PickMessageType.QUEUE)
            afterPickOfficial()
    }

    private suspend fun League.insertIntoDoc(pickData: PickData) {
        builder().let { b ->
            b.pickDoc(pickData)?.let { b }
        }?.execute()
    }

    context(InteractionData)
    private suspend fun League.formalPick(pokemon: DraftName, tier: String? = null, free: Boolean = false): Any? {
        if (isSwitchDraft && !config<AllowPickDuringSwitch>()) {
            return reply("Du kannst während des Switch-Drafts nicht picken!")
        }
        val mem = current
        val tierlist = tierlist
        val (tlName, official, _) = pokemon
        logger.info("tlName: $tlName, official: $official")
        val (specifiedTier, officialTier) = (getTierOf(tlName, tier)
            ?: return reply("Dieses Pokemon ist nicht in der Tierliste!"))
        checkUpdraft(specifiedTier, officialTier)?.let { return reply(it) }
        if (isPicked(official, officialTier)) return reply("Dieses Pokemon wurde bereits gepickt!")
        val tlMode = tierlist.mode
        val freepick =
            free.takeIf { tlMode.isTiersWithFree() && !(tierlist.variableMegaPrice && official.isMega) } ?: false
        if (!freepick && handleTiers(specifiedTier, officialTier)) return null
        if (handlePoints(
                free = freepick, tier = officialTier, mega = official.isMega
            )
        ) return null
        val saveTier = if (freepick) officialTier else specifiedTier
        lastPickedMon = pokemon
        val pickData = PickData(
            league = this,
            pokemon = tlName,
            pokemonofficial = official,
            tier = saveTier,
            mem = mem,
            round = getPickRoundOfficial(),
            freePick = freepick,
            updrafted = saveTier != officialTier
        )
        savePick(pickData)
        return pickData
    }

}
