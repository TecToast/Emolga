package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.AllowedData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into

object DraftPermissionCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("draftpermission", "Konfiguriert deine Ersatzdrafter")) {

    object Allow : CommandFeature<Allow.Args>(::Args, CommandSpec("allow", "Erlaube einer Person das Ersatzdraften")) {
        class Args : Arguments() {
            var user by member("User", "Der User, der für dich picken darf")
            var withmention by enumAdvanced<Mention>("Ping", "Wer soll gepingt werden?")
        }

        enum class Mention(override val prettyName: String, val selfmention: Boolean, val othermention: Boolean) :
            Nameable {
            ME("Nur ich", selfmention = true, othermention = false),
            OTHER("Nur andere Person", selfmention = false, othermention = true),
            BOTH("Beide", selfmention = true, othermention = true)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock({ db.leagueByCommand() },
                { iData.reply("Du nimmst nicht an einer Liga auf diesem Server teil!") }) l@{
                val mem = e.user
                if (mem.user.isBot) return@l iData.reply("Du kannst keine Bots als Ersatzdrafter hinzufügen!")
                val withMention = e.withmention
                val id = mem.idLong
                val set = performPermissionAdd(iData.user, id, withMention)
                iData.reply(embeds = Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                    description = set.sortedWith { o1, o2 ->
                        if (o1.u == iData.user) -1
                        else if (o2.u == iData.user) 1
                        else 0
                    }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                }.into())
                save()
            }
        }
    }

    context(league: League)
    fun performPermissionAdd(
        user: Long,
        toadd: Long,
        withMention: Allow.Mention,
        teammate: Boolean = false
    ): Set<AllowedData> {
        val set = league.allowed.getOrPut(league(user)) { mutableSetOf() }
        val selfmention = withMention.selfmention
        val othermention = withMention.othermention
        for ((userid, mention) in setOf(Pair(user, selfmention), Pair(toadd, othermention))) {
            (set.firstOrNull { it.u == userid }?.let { d -> d.mention = mention } ?: set.add(
                AllowedData(
                    userid,
                    mention,
                    teammate
                )
            ))
        }
        return set
    }

    object Deny : CommandFeature<Deny.Args>(::Args, CommandSpec("deny", "Verbiete einer Person das Ersatzdraften")) {
        class Args : Arguments() {
            var user by member("User", "Der User, der für dich picken darf")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(
                { db.leagueByCommand() },
                { iData.reply("Du nimmst nicht an einer Liga auf diesem Server teil!") }) l@{
                val mem = e.user
                if (mem.idLong == iData.user) return@l iData.reply("Du darfst tatsächlich immer picken :)")
                val set = allowed.getOrPut(this(iData.user)) { mutableSetOf() }
                set.removeIf { it.u == mem.idLong }
                iData.reply(embeds = Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                    description = set.sortedWith { o1, o2 ->
                        if (o1.u == iData.user) -1
                        else if (o2.u == iData.user) 1
                        else 0
                    }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                }.into())
                save()
            }
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
    }
}
