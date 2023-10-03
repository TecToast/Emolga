package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.DumbestFliesDB
import de.tectoast.emolga.utils.Constants.G.COMMUNITY
import de.tectoast.emolga.utils.DBF
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DumbestFliesCtlCommand : Command(
    "dumbestfliesctl", "dumbestfliesctl", CommandCategory.Flo
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, COMMUNITY)
        adminSlash = true
        setCustomPermissions(PermissionPreset.HENNY)
    }

    override suspend fun process(e: GuildCommandEvent) {}


    class Add : Command("add", "add") {
        init {
            argumentTemplate = ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User", ArgumentManagerTemplate.DiscordType.USER)
                .build()
        }

        override suspend fun process(e: GuildCommandEvent) {
            runCatching {
                transaction {
                    val mem = e.arguments.getMember("user")
                    DumbestFliesDB.insert {
                        it[id] = mem.idLong
                        it[name] = mem.effectiveName
                    }
                }
            }
            e.done(true)
        }
    }

    class Remove : Command("remove", "remove") {

        init {
            argumentTemplate = ArgumentManagerTemplate.create {
                add(
                    "user",
                    "User",
                    "Der User",
                    ArgumentManagerTemplate.DiscordType.USER
                )
            }
        }

        override suspend fun process(e: GuildCommandEvent) {
            transaction {
                DumbestFliesDB.deleteWhere { id eq e.arguments.getMember("user").idLong }
            }
            e.done(true)
        }
    }

    class List : Command("list", "list") {
        init {
            argumentTemplate = ArgumentManagerTemplate.noArgs()
        }

        override suspend fun process(e: GuildCommandEvent) {
            val list = transaction {
                DumbestFliesDB.selectAll().map { it[DumbestFliesDB.name] + " (<@${it[DumbestFliesDB.id]}>)" }
            }
            e.reply(list.joinToString("\n"), ephemeral = true)
        }
    }

    class Start : Command("start", "start") {
        init {
            argumentTemplate = ArgumentManagerTemplate.create {
                add("lifes", "Lifes", "Max Lifes", ArgumentManagerTemplate.Number.any(), optional = true)
            }
        }

        override suspend fun process(e: GuildCommandEvent) {
            DBF.initWithDB(e.arguments.getOrDefault("lifes", 3).toInt())
            e.done(true)
        }
    }

    class Tie : Command("tie", "tie") {
        init {
            argumentTemplate = ArgumentManagerTemplate.create {
                add("user", "User", "Der User", ArgumentManagerTemplate.DiscordType.USER)
            }
        }

        override suspend fun process(e: GuildCommandEvent) {
            DBF.realEndOfRound(e.slashCommandEvent!!, e.arguments.getMember("user").idLong)
        }


    }

    class StartQuestions : Command("startquestions", "startquestions") {
        init {
            argumentTemplate = ArgumentManagerTemplate.noArgs()
        }

        override suspend fun process(e: GuildCommandEvent) {
            DBF.startQuestions(e.slashCommandEvent!!)
        }
    }
}
