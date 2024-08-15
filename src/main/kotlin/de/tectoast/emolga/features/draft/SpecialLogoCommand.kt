package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.OptionType

object SpecialLogoCommand :
    CommandFeature<SpecialLogoCommand.Args>(
        ::Args,
        CommandSpec("speciallogo", "Speichert ein Logo in der Cloud")
    ) {

    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var logos by genericList<Message.Attachment, Message.Attachment>(
            "Logo %s",
            "Das Logo %s",
            12,
            0,
            OptionType.ATTACHMENT
        )
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        for (logoData in e.logos) {
            val logo = LogoCommand.LogoInputData.fromAttachment(logoData, ignoreRequirements = true) ?: return
            LogoCommand.uploadToCloud(logo, LogoCommand.LogoCloudHandler.Other)
            delay(3000)
        }
    }
}
