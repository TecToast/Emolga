package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.AllowedData
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Member

class DraftPermissionCommand : Command(
    "draftpermission", "Konfiguriert deine Ersatzdrafter", CommandCategory.Draft
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {}


    class Allow : Command("allow", "Erlaube einer Person das Ersatzdraften") {
        init {
            argumentTemplate = ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
                .add(
                    "withmention",
                    "Ping",
                    "Wer soll gepingt werden?",
                    ArgumentManagerTemplate.Text.of(
                        SubCommand("me", "Nur ich"),
                        SubCommand("other", "Nur andere Person"),
                        SubCommand("both", "Beide")
                    )
                ).build()
        }

        override suspend fun process(e: GuildCommandEvent) {
            val drafts = Emolga.get.drafts
            val gid = e.guild.idLong
            val author = e.author.idLong
            drafts.values.firstOrNull { it.guild == gid && author in it.table }?.let { l ->
                val mem = e.arguments.get<Member>("user")
                val withMention = e.arguments.get<String>("withmention")
                val id = mem.idLong
                val allowed = l.allowed
                val set = allowed.getOrPut(author) { mutableSetOf() }
                val selfmention = withMention == "me" || withMention == "both"
                val othermention = withMention == "other" || withMention == "both"
                for ((userid, mention) in setOf(Pair(author, selfmention), Pair(id, othermention))) {
                    (set.firstOrNull { it.u == userid }?.let { d -> d.mention = mention } ?: set.add(
                        AllowedData(
                            userid,
                            mention
                        )
                    ))
                }
                e.reply(Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                    description = set.sortedWith { o1, o2 ->
                        if (o1.u == author) -1
                        else if (o2.u == author) 1
                        else 0
                    }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                })
                saveEmolgaJSON()
            } ?: e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }

    class Deny : Command("deny", "Entziehe die Berechtigung fürs Ersatzdraften") {

        init {
            argumentTemplate = ArgumentManagerTemplate.create {
                add(
                    "user",
                    "User",
                    "Der User, der nicht mehr für dich picken darf",
                    ArgumentManagerTemplate.DiscordType.USER
                )
            }
        }

        override suspend fun process(e: GuildCommandEvent) {
            val drafts = Emolga.get.drafts
            val gid = e.guild.idLong
            val author = e.author.idLong
            drafts.values.firstOrNull { it.guild == gid && author in it.table }
                ?.let { l ->
                    val mem = e.arguments.get<Member>("user")
                    if (mem.idLong == author) return e.reply("Du darfst tatsächlich immer picken :)")
                    val set = l.allowed.getOrPut(author) { mutableSetOf() }
                    set.removeIf { it.u == mem.idLong }
                    e.reply(Embed(title = "Deine Draftberechtigungen", color = embedColor) {
                        description = set.sortedWith { o1, o2 ->
                            if (o1.u == author) -1
                            else if (o2.u == author) 1
                            else 0
                        }.joinToString("\n") { "<@${it.u}> (Mit Ping: ${if (it.mention) "ja" else "nein"})" }
                    })
                    saveEmolgaJSON()
                } ?: e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }
}
