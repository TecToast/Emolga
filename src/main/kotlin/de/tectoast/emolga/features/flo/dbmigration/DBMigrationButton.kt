package de.tectoast.emolga.features.flo.dbmigration

import de.tectoast.emolga.domain.maintenance.dbmigration.service.DatabaseMigrationService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DBMigrationButton(private val dbMigration: DatabaseMigrationService) :
    ButtonFeature<DBMigrationButton.Args>(::Args, ButtonSpec("dbmigration")) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = "Execute".k18n

    class Args : Arguments() {
        var id: String by string()
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        if (!dbMigration.executeMigration(e.id)) {
            return iData.replyRaw("Migration failed, id not found.")
        }
        iData.edit(contentK18n = null, components = this(disabled = true) { this.id = "" }.into())
    }
}