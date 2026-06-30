package de.tectoast.emolga.discord.jda.calendar

import de.tectoast.emolga.domain.guildspecific.calendar.service.bridge.CalendarNotificationSender
import de.tectoast.emolga.features.various.remind.RemindButton
import de.tectoast.emolga.utils.BotConstants
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.JDA
import org.koin.core.annotation.Single

@Single
class JDACalendarNotificationSender(
    private val jda: JDA,
    private val remindButton: RemindButton,
    private val botConstants: BotConstants
) : CalendarNotificationSender {
    override suspend fun sendReminder(message: String) {
        val calendarTc = jda.getTextChannelById(botConstants.calendarChannelId) ?: return
        calendarTc.sendMessage("(<@${botConstants.botOwnerId}>) $message")
            .addComponents(remindButton.withoutIData().into()).queue()
    }

    override suspend fun updateCalendarDisplay(content: String) {
        val calendarTc = jda.getTextChannelById(botConstants.calendarChannelId) ?: return
        calendarTc.editMessageById(botConstants.calendarMessageId, content).queue()
    }
}