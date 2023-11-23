package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.json.emolga.draft.isMega
import mu.KotlinLogging

@Suppress("unused")
object PickCommand : TestableCommand<PickCommandArgs>("pick", "Pickt das Pokemon") {

    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "pokemon",
            "Pokemon",
            "Das Pokemon, was du picken willst",
            draftPokemonArgumentType,
            false,
            "Das ist kein Pokemon!"
        ).add("tier", "Tier", "Das Tier", ArgumentManagerTemplate.Text.draftTiers(), true)
            .add("free", "Free-Pick", "Ob dieser Pick ein Freepick ist", ArgumentManagerTemplate.ArgumentBoolean, true)
            .setExample("!pick Emolga").build()
        slash(true, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = PickCommandArgs(
        e.arguments.getDraftName("pokemon"),
        e.arguments.getNullable("tier"),
        e.arguments.getOrDefault("free", false)
    )

    private val logger = KotlinLogging.logger {}

    context (CommandData)
    override suspend fun exec(
        e: PickCommandArgs
    ) {
        val dd = League.byCommand() ?: return run {
            if (!acknowledged) {
                reply(
                    "Es läuft zurzeit kein Draft in diesem Channel!",
                    ephemeral = true
                )
            }
        }
        val (d, data) = dd
        d.lockForPick(data) l@{
            if (d.isSwitchDraft && !d.allowPickDuringSwitch) {
                reply("Du kannst während des Switch-Drafts nicht picken!")
                return@l
            }
            d.beforePick()?.let { reply(it); return@l }
            val mem = d.current
            val tierlist = d.tierlist
            val picks = d.picks(mem)
            val (tlName, official, _) = e.pokemon
            println("tlName: $tlName, official: $official")
            val (specifiedTier, officialTier) =
                (d.getTierOf(tlName, e.tier)
                    ?: return@l reply("Dieses Pokemon ist nicht in der Tierliste!"))

            d.checkUpdraft(specifiedTier, officialTier)?.let { return@l reply(it) }
            if (d.isPicked(official, officialTier)) return@l reply("Dieses Pokemon wurde bereits gepickt!")
            val tlMode = tierlist.mode
            val free = e.free
                .takeIf { tlMode.isTiersWithFree() && !(tierlist.variableMegaPrice && official.isMega) } ?: false
            if (!free && d.handleTiers(specifiedTier, officialTier)) return@l
            if (d.handlePoints(
                    tlNameNew = tlName,
                    officialNew = official,
                    free = free,
                    tier = officialTier
                )
            ) return@l
            val saveTier = if (free) officialTier else specifiedTier
            d.savePick(picks, official, saveTier, free)
            //m.delete().queue();
            if (!e.random) d.replyPick(tlName, free, specifiedTier.takeIf { saveTier != officialTier })
            if (e.random) {
                d.replyRandomPick(tlName, specifiedTier)
            } else if (official == "Emolga") {
                sendMessage("<:Happy:967390966153609226> ".repeat(5))
            }
            val round = d.getPickRoundOfficial()
            with(d) {
                builder().let { b ->
                    b.pickDoc(
                        PickData(
                            league = d,
                            pokemon = tlName,
                            pokemonofficial = official,
                            tier = saveTier,
                            mem = mem,
                            indexInRound = indexInRound(round),
                            changedIndex = picks.indexOfFirst { it.name == official },
                            picks = picks,
                            round = round,
                            memIndex = table.indexOf(mem),
                            freePick = free
                        )
                    )?.let { b }
                }?.execute()
            }
            d.afterPickOfficial()
        }
    }

}

data class PickCommandArgs(
    val pokemon: DraftName,
    val tier: String? = null,
    val free: Boolean = false,
    val random: Boolean = false
) : CommandArgs
