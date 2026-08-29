package de.tectoast.emolga.features.flo.priv

import de.tectoast.emolga.domain.discord.service.GeneralDiscordService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class MaintenanceCommand(enable: Enable, disable: Disable) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("maintenance", "maintenance".k18n)) {
    override val children = listOf(enable, disable)

    @Single
    class Enable(private val service: GeneralDiscordService) :
        CommandFeature<Enable.Args>(::Args, CommandSpec("enable", "enable".k18n)) {

        class Args : Arguments() {
            var reason by string("reason", "reason".k18n).nullable()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            service.enableMaintenance(e.reason ?: GeneralDiscordService.ROUTINE_MAINTENANCE_KEY)
            iData.replyRaw("Maintenance mode enabled")
        }
    }

    @Single
    class Disable(private val service: GeneralDiscordService) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", "disable".k18n)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            service.disableMaintenance()
            iData.replyRaw("Maintenance mode disabled")
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}