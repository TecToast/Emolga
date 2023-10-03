package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PrivateCommands
import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member

object GrabIDCommand : Command("grabid", "grabid", CommandCategory.Flo) {

    private val logger = KotlinLogging.logger {}
    private val cachedMembers = mutableSetOf<Member>()

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("name", "Name", "Name", ArgumentManagerTemplate.Text.withAutocomplete { s, event ->
                if (s.isBlank() || s.length < 4) return@withAutocomplete null
                logger.info("checking {}", s)
                event.jda.getGuildById(PrivateCommands.guildForUserIDGrabbing!!)!!.retrieveMembersByPrefix(s, 100)
                    .await().also { cachedMembers += it }.map { it.effectiveName }.takeIf { it.size <= 25 }
            }, false)
        }
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val x: String = e.arguments["name"]
        val id = x.toLongOrNull()
        PrivateCommands.grabbedIDs += cachedMembers.first { mem ->
            (id?.let { mem.idLong == it }) ?: (mem.effectiveName == x)
        }.idLong
        e.done(ephemeral = true)
    }
}
