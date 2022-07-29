package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject

class AllowCommand : Command(
    "allow", "Erlaubt einem anderen User, für dich zu picken", CommandCategory.Draft, Constants.ASLID, Constants.NDSID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!allow @Flo").build()
    }

    override fun process(e: GuildCommandEvent) {
        val drafts = emolgaJSON.getJSONObject("drafts")
        val member = e.member
        (if (e.guild.idLong == Constants.NDSID) drafts.getJSONObject("NDS") else drafts.keySet().asSequence()
            .map { drafts.getJSONObject(it) }.filter { it.has("guild") }.filter { it.getString("guild") == e.guild.id }
            .filter { it.has("table") }.filter { member.idLong in it.getLongList("table") }.firstOrNull())?.let {
            val mem = e.arguments.getMember("user")
            val user = mem.idLong
            if (!it.has("allowed")) it.put("allowed", JSONObject())
            val allowed = it.getJSONObject("allowed")
            if (allowed.has(user)) {
                e.guild.retrieveMemberById(allowed.getLong(user))
                    .queue { m -> e.reply("${mem.effectiveName} pickt bereits für `$m`!") }
                return
            }
            allowed.put(user, member.idLong)
            e.reply("Du hast ${mem.effectiveName} erlaubt, für dich zu picken!")
            saveEmolgaJSON()
        } ?: e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")

    }
}