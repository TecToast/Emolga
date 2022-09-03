package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.AllowedData
import net.dv8tion.jda.api.entities.Member

class DraftPermissionCommand : Command(
    "draftpermission", "Konfiguriert deine Ersatzdrafter", CommandCategory.Draft
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) {}


    class Allow : Command("allow", "Erlaube einer Person das Ersatzdraften") {
        init {
            argumentTemplate = ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
                .add(
                    "withmention",
                    "Mit Tag",
                    "Ob diese Person dann auch statt dir getaggt wird",
                    ArgumentManagerTemplate.ArgumentBoolean
                ).build()
        }

        override suspend fun process(e: GuildCommandEvent) {
            val drafts = Emolga.get.drafts
            val gid = e.guild.idLong
            val author = e.author.idLong
            drafts.values.firstOrNull { it.guild == gid && author in it.table }
                ?.let { l ->
                    val mem = e.arguments.get<Member>("user")
                    val withMention = e.arguments.get<Boolean>("withmention")
                    val id = mem.idLong
                    val allowed = l.allowed
                    allowed[author]?.firstOrNull { it.u == id }?.let {
                        if (it.mention == withMention) {
                            e.reply("Diese Einstellung ist bereits getroffen!")
                            return
                        }
                        e.reply("${mem.effectiveName} wird nun ${if (withMention) "" else "nicht mehr "}getaggt!")
                        it.mention = withMention
                        saveEmolgaJSON()
                        return
                    }
                    val allo = allowed.getOrPut(author) { mutableListOf() }
                    if (withMention) allo.forEach { it.mention = false }
                    allo.add(AllowedData(id, withMention))
                    e.reply("Du hast ${mem.effectiveName} erlaubt, für dich zu picken! ${if (withMention) "Außerdem wird diese Person statt dir gepingt!" else ""}")
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
                    if (l.allowed[author]?.removeIf { it.u == mem.idLong } != true) {
                        e.reply("${mem.effectiveName} darf zurzeit nicht für dich picken!")
                        return
                    }
                    e.reply("${mem.effectiveName} darf nun nicht mehr für dich picken!")
                    saveEmolgaJSON()
                } ?: e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!")
        }
    }
}