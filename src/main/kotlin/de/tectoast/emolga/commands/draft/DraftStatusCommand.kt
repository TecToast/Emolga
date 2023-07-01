package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Member

class DraftStatusCommand :
    Command("draftstatus", "Zeigt Informationen für den aktuellen Draft an", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "user",
                "User",
                "Der User, dessen Status angezeigt werden soll",
                ArgumentManagerTemplate.DiscordType.USER,
                true
            )
        }
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        var externalName: String? = null
        val mem =
            e.arguments.getNullable<Member>("user")?.also { externalName = it.effectiveName }?.idLong ?: e.author.idLong
        val external = mem != e.author.idLong
        val d = db.leagueByGuild(e.guild.idLong, mem) ?: return e.reply(
            if (external) "<@$mem> nimmt nicht an einer Liga auf diesem Server teil!" else "Du nimmst nicht an einer Liga auf diesem Server teil!",
            ephemeral = true
        )
        if (!d.isRunning) return e.reply("Der Draft läuft zurzeit nicht!", ephemeral = true)
        with(d) {
            val tl = tierlist
            val order = tl.order
            val mode = tl.mode
            if (mem !in table) return e.reply(
                if (external) "<@$mem> nimmt nicht an der Liga teil!" else "Du nimmst nicht an der Liga teil!",
                ephemeral = true
            )
            e.reply(
                Embed(
                    title = if (external) "Draft-Status von $externalName" else "Dein Draft-Status", color = embedColor
                ) {
                    field {
                        name = "Bisheriger Kader"
                        value = picks[mem]?.sortedWith(compareBy({ it.tier.indexedBy(order) }, { it.name }))
                            ?.joinToString("\n") { it.tier + ": " + it.name }?.takeUnless { it.isBlank() } ?: "_nichts_"
                        inline = false
                    }
                    if (mode.withPoints) field {
                        name = "Mögliche Punkte"
                        value = points[mem].toString()
                        inline = false
                    }
                    if (mode.withTiers) field {
                        name = "Mögliche Tiers"
                        value = getPossibleTiersAsString(mem)
                        inline = false
                    }
                }, ephemeral = true
            )
        }
    }
}
