package de.tectoast.emolga.features.various.remind

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RemindButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("remind")) {
    override val label = "Löschen".k18n

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.replyRaw(":D", ephemeral = true)
        iData.hook.deleteMessageById(iData.data.messageId!!)
    }
}
