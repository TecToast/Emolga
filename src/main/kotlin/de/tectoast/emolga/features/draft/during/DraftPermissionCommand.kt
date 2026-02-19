package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.draft.during.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.league.AllowedData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into

object DraftPermissionCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("draftpermission", K18n_DraftPermission.Help)) {

    object Allow : CommandFeature<Allow.Args>(::Args, CommandSpec("allow", K18n_DraftPermission.AllowHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_DraftPermission.AllowArgUser)
            var withmention by enumAdvanced<Mention>("Ping", K18n_DraftPermission.AllowArgMention)
        }

        enum class Mention(override val prettyName: K18nMessage, val selfmention: Boolean, val othermention: Boolean) :
            Nameable {
            ME(K18n_DraftPermission.AllowMentionMe, selfmention = true, othermention = false),
            OTHER(K18n_DraftPermission.AllowMentionOther, selfmention = false, othermention = true),
            BOTH(K18n_DraftPermission.AllowMentionBoth, selfmention = true, othermention = true)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(
                { mdb.leagueByCommand() },
                { iData.reply(K18n_NoLeagueForGuildFound) }) l@{
                val mem = e.user
                if (mem.user.isBot) return@l iData.reply(K18n_DraftPermission.NoBotsAllowed)
                val withMention = e.withmention
                val id = mem.idLong
                val set = performPermissionAdd(iData.user, id, withMention)
                iData.reply(embeds = Embed(title = K18n_DraftPermission.EmbedTitle.t(), color = embedColor) {
                    description = set.toDescription(iData.user)
                }.into(), ephemeral = true)
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

    object Deny : CommandFeature<Deny.Args>(::Args, CommandSpec("deny", K18n_DraftPermission.DenyHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_DraftPermission.DenyArgUser)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(
                { mdb.leagueByCommand() },
                { iData.reply(K18n_NoLeagueForGuildFound) }) l@{
                val mem = e.user
                if (mem.idLong == iData.user) return@l iData.reply(K18n_DraftPermission.DenyYourself)
                val set = allowed.getOrPut(this(iData.user)) { mutableSetOf() }
                set.removeIf { it.u == mem.idLong }
                iData.reply(embeds = Embed(title = K18n_DraftPermission.EmbedTitle.t(), color = embedColor) {
                    description = set.toDescription(iData.user)
                }.into(), ephemeral = true)
                save()
            }
        }
    }

    context(iData: InteractionData)
    fun Set<AllowedData>.toDescription(user: Long) = sortedWith { o1, o2 ->
        if (o1.u == user) -1
        else if (o2.u == user) 1
        else 0
    }.joinToString("\n") { (if (it.mention) K18n_DraftPermission.EmbedYes(it.u) else K18n_DraftPermission.EmbedNo(it.u)).t() }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
    }
}
