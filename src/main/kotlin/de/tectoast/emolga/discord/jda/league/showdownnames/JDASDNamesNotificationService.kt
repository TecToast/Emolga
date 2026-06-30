package de.tectoast.emolga.discord.jda.league.showdownnames

import de.tectoast.emolga.domain.league.showdownnames.model.ShowdownUserID
import de.tectoast.emolga.domain.league.showdownnames.service.bridge.SDNamesNotificationService
import de.tectoast.emolga.features.flo.SDNamesApprovalButton
import de.tectoast.generic.K18n_No
import de.tectoast.generic.K18n_Yes
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class JDASDNamesNotificationService(
    private val jda: JDA
) : SDNamesNotificationService, KoinComponent {
    private val btn: SDNamesApprovalButton by inject()
    override suspend fun sendApprovalNotification(
        name: String,
        showdownUserId: ShowdownUserID,
        id: Long,
        currentOwner: Long
    ) {
        jda.getTextChannelById(SDNAMES_CHANNEL_ID)!!.send(
            "<@$id> [$id] (`${
                jda.retrieveUserById(id).await().effectiveName
            }`) möchte den Namen `$name` [`$showdownUserId`] haben, aber dieser ist bereits von " +
                    "<@$currentOwner> [$currentOwner] (`${
                        jda.retrieveUserById(currentOwner).await().effectiveName
                    }`) belegt! Akzeptieren?",
            components = listOf(
                btn.withoutIData(label = K18n_Yes, buttonStyle = ButtonStyle.SUCCESS) {
                    accept = true; this.id = id; this.username = showdownUserId.value
                },
                btn.withoutIData(
                    label = K18n_No,
                    buttonStyle = ButtonStyle.DANGER
                ) { accept = false }
            ).into()
        ).queue()
    }
}

private const val SDNAMES_CHANNEL_ID = 1148173270726737961