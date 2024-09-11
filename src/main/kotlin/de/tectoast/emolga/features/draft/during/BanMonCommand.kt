package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
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
        League.executePickLike {
            if (this !is Prisma) return reply("Dieser Command funktioniert nur im Prisma Draft!")
            val (tlName, official, _) = e.pokemon
            val (specifiedTier, officialTier) =
                (getTierOf(tlName, null)
                    ?: return@executePickLike reply("Dieses Pokemon ist nicht in der Tierliste!"))
            checkUpdraft(specifiedTier, officialTier)?.let { return@executePickLike reply(it) }
            if (isPicked(official, officialTier)) return@executePickLike reply("Dieses Pokemon wurde bereits gepickt!")
            if (official in bannedMons) return@executePickLike reply("Dieses Pokemon wurde bereits gebannt!")
            bannedMons.add(official)
            replyGeneral("$tlName gebannt!")
            builder().apply { banDoc(e) }.execute()
            afterPickOfficial()
        }
    }
}
