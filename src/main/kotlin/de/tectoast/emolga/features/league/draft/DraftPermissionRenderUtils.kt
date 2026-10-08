package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.member.model.LeagueParticipant
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.entities.MessageEmbed

context(iData: InteractionData)
fun List<LeagueParticipant>.toEmbed(title: K18nMessage): List<MessageEmbed> {
    return Embed(title = title.t(), color = Constants.EMBED_COLOR) {
        description = joinToString("\n") {
            (if (it.shouldPing) K18n_DraftPermission.EmbedYes(it.userId) else K18n_DraftPermission.EmbedNo(it.userId)).t()
        }
    }.into()
}

