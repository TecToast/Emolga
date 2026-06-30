package de.tectoast.emolga.features.flo.dbmigration

import de.tectoast.emolga.di.DiscordReadyTask
import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.maintenance.dbmigration.service.DatabaseMigrationService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class, DiscordReadyTask::class])
class DBMigrationCommand(
    private val dbMigration: DatabaseMigrationService,
    private val btn: DBMigrationButton,
    private val channelInterface: ChannelInterface
) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("dbmigration", "DB Migration".k18n)), DiscordReadyTask {
    private val initialMigrationScope = createCoroutineScope("InitialMigration", Dispatchers.IO)

    override suspend fun onDiscordReady() {
        initialMigrationScope.launch {
            sendMigrationStatements(447357526997073932)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.done(true)
        sendMigrationStatements(iData.tc)
    }

    private suspend fun sendMigrationStatements(channel: Long) {
        val (id, statements) = dbMigration.generateMigrationPlan() ?: return
        channelInterface.sendMessage(
            channel,
            MessageCreate(
                statements.joinToString(
                    separator = "\n",
                    prefix = "```sql\n",
                    postfix = "\n```"
                ) { "$it;" },
                components = btn.withoutIData {
                    this.id = id
                }.into()
            )
        )
    }
}

