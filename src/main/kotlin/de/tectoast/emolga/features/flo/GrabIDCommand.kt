package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.Member

object GrabIDCommand : CommandFeature<GrabIDCommand.Args>(::Args, CommandSpec("grabid", "grabid".k18n)) {
    init {
        restrict(flo)
    }

    private val cachedMembers = mutableSetOf<Member>()

    class Args : Arguments() {
        var name by string("Name", "Name".k18n) {
            slashCommand { s, event ->
                if (s.isBlank() || s.length < 4) return@slashCommand null
                logger.info("checking {}", s)
                event.jda.getGuildById(PrivateCommands.guildForUserIDGrabbing!!)!!.retrieveMembersByPrefix(s, 100)
                    .await().also { cachedMembers += it }.map { it.effectiveName }.takeIf { it.size <= 25 }
            }
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val x = e.name
        val id = x.toLongOrNull()
        PrivateCommands.grabbedIDs += cachedMembers.first { mem ->
            (id?.let { mem.idLong == it }) ?: (mem.effectiveName == x)
        }.idLong
        iData.done(true)
    }
}
