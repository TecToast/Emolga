package de.tectoast.emolga.features.flo

import de.tectoast.emolga.domain.league.teamgraphic.service.TeamGraphicUpdateService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.MessageContextArgs
import de.tectoast.emolga.features.system.MessageContextSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.MessageContextFeature
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class UpdateTeamGraphic(private val service: TeamGraphicUpdateService) :
    MessageContextFeature(MessageContextSpec("Update Teamgraphic (DEV ONLY)")) {
    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        iData.replyRaw(service.updateTeamGraphic(e.message.idLong)?.let { "Updating teamgraphic..." }
            ?: "No teamgraphic found for ${e.message.idLong}", ephemeral = true)
    }
}