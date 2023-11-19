package de.tectoast.emolga.managers

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import net.dv8tion.jda.api.entities.emoji.Emoji

object SignupManager {
    suspend fun createSignup(
        announceChannel: Long,
        signupChannel: Long,
        logoChannel: Long,
        maxUsers: Int,
        roleId: Long?,
        withExperiences: Boolean,
        text: String
    ) {
        val tc = jda.getTextChannelById(announceChannel)!!
        val messageid =
            tc.sendMessage(
                text + "\n\n**Teilnehmer: 0/${maxUsers.takeIf { it > 0 } ?: "?"}**")
                .addActionRow(
                    primary(
                        "signup", "Anmelden", Emoji.fromUnicode("✅")
                    )
                ).await().idLong
        db.signups.insertOne(
            LigaStartData(
                guild = tc.guild.idLong,
                signupChannel = signupChannel,
                logoChannel = logoChannel,
                maxUsers = maxUsers,
                announceChannel = tc.idLong,
                announceMessageId = messageid,
                participantRole = roleId,
                signupMessage = text,
                withExperiences = withExperiences
            )
        )
    }
}
