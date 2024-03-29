package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.AllowedData
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import org.litote.kmongo.contains
import org.litote.kmongo.eq

object DraftPermissionCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("draftpermission", "Konfiguriert deine Ersatzdrafter", *draftGuilds)) {

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

        context(InteractionData)
        override suspend fun exec(e: Args) {
            db.drafts.find(League::guild eq gid, League::table contains user).first()?.let { l ->
                val mem = e.user
                if (mem.user.isBot) return reply("Du kannst keine Bots als Ersatzdrafter hinzufügen!")
                val withMention = e.withmention
                val id = mem.idLong
                val allowed = l.allowed
                val set = allowed.getOrPut(user) { mutableSetOf() }
                val selfmention = withMention.selfmention
                val othermention = withMention.othermention
                for ((userid, mention) in setOf(Pair(user, selfmention), Pair(id, othermention))) {
                    (set.firstOrNull { it.u == userid }?.let { d -> d.mention = mention } ?: set.add(
                        AllowedData(
                            userid,
                            mention
                        )
                    ))
                }
                reply(embeds = Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                    description = set.sortedWith { o1, o2 ->
                        if (o1.u == user) -1
                        else if (o2.u == user) 1
                        else 0
                    }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                }.into())
                l.save()
            } ?: reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }

    object Deny : CommandFeature<Deny.Args>(::Args, CommandSpec("deny", "Verbiete einer Person das Ersatzdraften")) {
        class Args : Arguments() {
            var user by member("User", "Der User, der für dich picken darf")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            db.drafts.find(League::guild eq gid, League::table contains user).first()?.let { l ->
                val mem = e.user
                if (mem.idLong == user) return reply("Du darfst tatsächlich immer picken :)")
                val set = l.allowed.getOrPut(user) { mutableSetOf() }
                set.removeIf { it.u == mem.idLong }
                reply(embeds = Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                    description = set.sortedWith { o1, o2 ->
                        if (o1.u == user) -1
                        else if (o2.u == user) 1
                        else 0
                    }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                }.into())
                l.save()
            } ?: reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
    }
}
