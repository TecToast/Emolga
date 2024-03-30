package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.json.emolga.draft.isMega
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
            slashCommand(disabledGuilds = setOf(Constants.G.VIP))
        }
        var random by boolean("random", "RANDOMPICK (not visible)") {
            onlyInCode = true
            default = false
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val dd = League.byCommand() ?: return run {
            if (!replied) {
                reply(
                    "Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true
                )
            }
        }
        val (d, data) = dd
        d.lockForPick(data) l@{
            executeWithinLock(e.pokemon, e.tier, e.free, e.random, fromQueue = false)
        }
    }

    context(InteractionData)
    suspend fun League.executeWithinLock(
        pokemon: DraftName,
        tier: String?,
        free: Boolean,
        random: Boolean,
        fromQueue: Boolean
    ) {
        val pickData = formalPick(pokemon, tier, free) as? PickData ?: return
        respond(pickData, random, fromQueue)
        insertIntoDoc(pickData)
        if (!fromQueue)
            afterPickOfficial()
    }

    private suspend fun League.insertIntoDoc(pickData: PickData) {
        builder().let { b ->
            b.pickDoc(pickData)?.let { b }
        }?.execute()
    }

    context(InteractionData)
    private suspend fun League.respond(
        pickData: PickData, random: Boolean = false, fromQueue: Boolean
    ) {
        if (fromQueue) {
            tc.sendMessage("**${getCurrentName()}** hat ${pickData.displayName()} aus der Queue gepickt!").queue()
        } else {
            if (!random) replyPick(pickData)
            if (random) {
                replyRandomPick(pickData)
            } else if (pickData.pokemonofficial == "Emolga") {
                sendMessage("<:Happy:967390966153609226> ".repeat(5))
            }
        }
    }

    context(InteractionData)
    private suspend fun League.formalPick(pokemon: DraftName, tier: String? = null, free: Boolean = false): Any? {
        if (isSwitchDraft && !allowPickDuringSwitch) {
            return reply("Du kannst während des Switch-Drafts nicht picken!")
        }
        beforePick()?.let { return reply(it) }
        val mem = current
        val tierlist = tierlist
        val picks = picks(mem)
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
                tlNameNew = tlName, officialNew = official, free = freepick, tier = officialTier
            )
        ) return null
        val saveTier = if (freepick) officialTier else specifiedTier
        savePick(picks, official, saveTier, freepick)
        lastPickedMon = pokemon
        return PickData(
            league = this,
            pokemon = tlName,
            pokemonofficial = official,
            tier = saveTier,
            mem = mem,
            round = getPickRoundOfficial(),
            freePick = free,
            updrafted = saveTier != officialTier
        )
    }

}
