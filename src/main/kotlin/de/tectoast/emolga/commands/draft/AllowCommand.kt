package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject
import java.util.*

class AllowCommand : Command(
    "allow",
    "Erlaubt einem anderen User, für dich zu picken",
    CommandCategory.Draft,
    Constants.ASLID,
    Constants.NDSID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!allow @Flo")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val drafts = emolgaJSON.getJSONObject("drafts")
        val member = e.member
        val op = if (e.guild.idLong == Constants.NDSID) Optional.of(drafts.getJSONObject("NDS")) else drafts.keySet()
            .stream().map { key: String? -> drafts.getJSONObject(key) }
            .filter { o: JSONObject -> o.has("guild") }.filter { o: JSONObject -> o.getString("guild") == e.guild.id }
            .filter { o: JSONObject -> o.has("table") }
            .filter { o: JSONObject -> o.getLongList("table").contains(member.idLong) }.findFirst()
        if (op.isPresent) {
            val mem = e.arguments!!.getMember("user")
            val user = mem.idLong
            val league = op.get()
            if (!league.has("allowed")) league.put("allowed", JSONObject())
            val allowed = league.getJSONObject("allowed")
            if (allowed.has(user)) {
                e.reply(
                    "%s pickt bereits für `%s`!".formatted(
                        mem.effectiveName,
                        e.guild.retrieveMemberById(allowed.getLong(user)).complete().effectiveName
                    )
                )
                return
            }
            allowed.put(user, member.idLong as Any)
            e.reply("Du hast %s erlaubt, für dich zu picken!".formatted(mem.effectiveName))
            saveEmolgaJSON()
        } else {
            e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }
}