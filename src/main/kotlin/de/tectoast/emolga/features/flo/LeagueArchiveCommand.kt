package de.tectoast.emolga.features.flo

import de.tectoast.emolga.domain.league.archive.service.LeagueArchiveService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LeagueArchiveCommand(private val service: LeagueArchiveService) :
    CommandFeature<LeagueArchiveCommand.Args>(::Args, CommandSpec("leaguearchive", "Archive League".k18n)) {

    class Args : Arguments() {
        var guild by long("guild", "guild".k18n)
    }

    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        service.archiveGuild(e.guild)
        iData.done()
    }
}
