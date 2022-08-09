package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga
import net.dv8tion.jda.api.entities.Member

class AllowCommand : Command(
    "allow", "Erlaubt einem anderen User, für dich zu picken", CommandCategory.Draft, Constants.ASLID, Constants.NDSID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!allow @Flo").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val drafts = Emolga.get.drafts
        val gid = e.guild.idLong
        (if (gid == Constants.NDSID) Emolga.get.nds() else drafts.values.firstOrNull { it.guild == gid })?.takeIf { e.author.idLong in it.picks }
            ?.let { l ->
                val mem = e.arguments.get<Member>("user")
                val id = mem.idLong
                val allowed = l.allowed
                allowed[id]?.let { picker ->
                    e.guild.retrieveMemberById(picker)
                        .queue { e.reply("${mem.effectiveName} pickt bereits für `${it.effectiveName}`") }
                    return
                }
                allowed[id] = e.member.idLong
                e.reply("Du hast ${mem.effectiveName} erlaubt, für dich zu picken!")
                saveEmolgaJSON()
            } ?: e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
    }
}