package de.tectoast.emolga.discord.jda.features

import de.tectoast.emolga.features.interaction.InteractionDataHook
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.interactions.InteractionHook

class JDAInteractionDataHook(private val jdaHook: InteractionHook) : InteractionDataHook {
    override suspend fun deleteMessageById(id: Long) {
        jdaHook.deleteMessageById(id).await()
    }
}