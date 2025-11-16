package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.LogoInputData
import de.tectoast.emolga.utils.json.unwrap
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

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        for (logoData in e.logos) {
            val logo = LogoInputData.fromAttachment(logoData, ignoreRequirements = true).unwrap()
            LogoCommand.uploadToCloud(logo) {
                iData.jda.getTextChannelById(447357526997073932)!!.sendMessage(it.url).queue()
            }
            delay(3000)
        }
    }
}
