package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import mu.KotlinLogging

@Suppress("unused")
class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {

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

    override suspend fun process(e: GuildCommandEvent) = exec(e, false)


    companion object {
        private val logger = KotlinLogging.logger {}

        suspend fun exec(
            e: GuildCommandEvent, isRandom: Boolean
        ) {
            val args = e.arguments
            val d = League.byCommand(e) ?: return e.reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
            if (d.isSwitchDraft && !d.allowPickDuringSwitch) {
                e.reply("Du kannst während des Switch-Drafts nicht picken!")
                return
            }
            val mem = d.current
            val tierlist = d.tierlist
            d.beforePick()?.let { e.reply(it); return }
            val picks = d.picks(mem)
            val (tlName, official, _) = args.getDraftName("pokemon")
            println("tlName: $tlName, official: $official")
            val (specifiedTier, officialTier) = try {
                d.getTierOf(tlName, args.getNullable("tier"))
            } catch (ex: NoSuchElementException) {
                return e.reply("Dieses Pokemon ist nicht in der Tierliste!")
            }
            d.checkUpdraft(e, specifiedTier, officialTier)?.let { e.reply(it); return }
            if (d.isPicked(official, officialTier)) return e.reply("Dieses Pokemon wurde bereits gepickt!")
            val tlMode = tierlist.mode
            val free = args.getOrDefault("free", false).takeIf { tlMode.isTiersWithFree() } ?: false
            if (!free && d.handleTiers(e, specifiedTier, officialTier)) return
            if (d.handlePoints(e, tlName, free)) return
            d.savePick(picks, official, specifiedTier, free)
            //m.delete().queue();
            if (!isRandom) d.replyPick(e, tlName, free, specifiedTier.takeIf { specifiedTier != officialTier })
            if (isRandom) {
                d.replyRandomPick(e, tlName, specifiedTier)
            } else if (official == "Emolga") {
                e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                    .queue()
            }
            val round = d.getPickRoundOfficial()
            with(d) {
                builder().let { b ->
                    b.pickDoc(
                        PickData(
                            league = d,
                            pokemon = tlName,
                            tier = specifiedTier,
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
