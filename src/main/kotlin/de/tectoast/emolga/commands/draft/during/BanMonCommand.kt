package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.CommandArgs
import de.tectoast.emolga.commands.CommandData
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.TestableCommand
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.utils.json.emolga.draft.BypassCurrentPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.Prisma

object BanMonCommand : TestableCommand<BanCommandArgs>("banmon", "Bannt ein Mon im Pick&Ban-System") {
    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("pokemon", "Pokemon", "Das Pokemon, welches du bannen möchtest", draftPokemonArgumentType)
        }
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = BanCommandArgs(e.arguments.getDraftName("pokemon"))

    context(CommandData)
    override suspend fun exec(e: BanCommandArgs) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        if (d !is Prisma) return reply("Dieser Command funktioniert nur im Prisma Draft!")
        d.lockForPick(BypassCurrentPlayerData.No) l@{

            val (tlName, official) = e.pokemon
            val (specifiedTier, officialTier) =
                (d.getTierOf(tlName, null)
                    ?: return@l reply("Dieses Pokemon ist nicht in der Tierliste!"))

            d.checkUpdraft(specifiedTier, officialTier)?.let { return@l reply(it) }
            if (d.isPicked(official, officialTier)) return@l reply("Dieses Pokemon wurde bereits gepickt!")
        }
    }
}

data class BanCommandArgs(
    val pokemon: DraftName
) : CommandArgs
