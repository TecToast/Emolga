package de.tectoast.emolga.features.flegmon.roles

import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.model.AcceptRulesResult
import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.service.RuleAcceptService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class RuleAcceptButton(private val service: RuleAcceptService) :
    ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("ruleaccept")) {
    override val label = "Regeln akzeptieren".k18n

    override val buttonStyle = ButtonStyle.PRIMARY

    override val emoji = Emoji.fromUnicode("✅")

    context (iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        val result = service.acceptRules(iData.gid, iData.user, iData.data.memberRoles)
        iData.replyRaw(
            when (result) {
                AcceptRulesResult.AlreadyAccepted -> "Du hast die Regeln bereits akzeptiert!"
                AcceptRulesResult.Accepted -> "Du hast die Regeln akzeptiert und hast jetzt Zugriff auf den Server!\nWeitere optionale Rollen kannst du dir in <#1243646697242890373> abholen."
            }, ephemeral = true
        )
    }
}


