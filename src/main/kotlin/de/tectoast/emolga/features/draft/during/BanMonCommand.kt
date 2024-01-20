package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.BypassCurrentPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.Prisma

object BanMonCommand : CommandFeature<BanMonCommand.Args>(
    ::Args,
    CommandSpec("banmon", "Bannt ein Mon im Pick&Ban-System", Constants.G.FLP)
) {
    class Args : Arguments() {
        var pokemon by draftPokemon("Pokemon", "Das Pokemon, welches du bannen möchtest")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        if (d !is Prisma) return reply("Dieser Command funktioniert nur im Prisma Draft!")
        d.lockForPick(BypassCurrentPlayerData.No) l@{

            val (tlName, official, _) = e.pokemon
            val (specifiedTier, officialTier) =
                (d.getTierOf(tlName, null)
                    ?: return@l reply("Dieses Pokemon ist nicht in der Tierliste!"))
            d.checkUpdraft(specifiedTier, officialTier)?.let { return@l reply(it) }
            if (d.isPicked(official, officialTier)) return@l reply("Dieses Pokemon wurde bereits gepickt!")
            if (official in d.bannedMons) return@l reply("Dieses Pokemon wurde bereits gebannt!")
            d.bannedMons.add(official)
            d.replyGeneral("$tlName gebannt!")
            with(d) {
                builder().apply { banDoc(e) }.execute()
            }
            d.afterPickOfficial()
        }
    }
}
