package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into

object DraftStatusCommand : CommandFeature<DraftStatusCommand.Args>(
    ::Args,
    CommandSpec("draftstatus", "Zeigt Informationen für den aktuellen Draft an", *draftGuilds)
) {
    class Args : Arguments() {
        var user by member("User", "Der User, dessen Status angezeigt werden soll").nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        var externalName: String? = null
        val mem =
            e.user?.also { externalName = it.effectiveName }?.idLong ?: iData.user
        val external = mem != iData.user
        val d = db.leagueByGuild(iData.gid, mem) ?: return iData.reply(
            if (external) "<@$mem> nimmt nicht an einer Liga auf diesem Server teil!" else "Du nimmst nicht an einer Liga auf diesem Server teil!",
            ephemeral = true
        )
        if (!d.isRunning) return iData.reply("Der Draft läuft zurzeit nicht!", ephemeral = true)
        with(d) {
            val tl = tierlist
            val order = tl.order
            val mode = tl.mode
            if (mem !in table) return iData.reply(
                if (external) "<@$mem> nimmt nicht an der Liga teil!" else "Du nimmst nicht an der Liga teil!",
                ephemeral = true
            )
            val idx = this(mem)
            iData.reply(
                embeds = Embed(
                    title = if (external) "Draft-Status von $externalName" else "Dein Draft-Status", color = embedColor
                ) {
                    field {
                        name = "Bisheriger Kader"
                        value = picks[idx]?.sortedWith(compareBy({ it.tier.indexedBy(order) }, { it.name }))
                            ?.joinToString("\n") { it.tier + ": " + it.name }?.takeUnless { it.isBlank() } ?: "_nichts_"
                        inline = false
                    }
                    if (mode.withPoints) field {
                        name = "Mögliche Punkte"
                        value = points[idx].toString()
                        inline = false
                    }
                    if (mode.withTiers) field {
                        name = "Mögliche Tiers"
                        value = getPossibleTiersAsString(idx)
                        inline = false
                    }
                }.into(), ephemeral = true
            )
        }
    }
}
