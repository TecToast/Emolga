package de.tectoast.emolga.discord.jda.provider

import de.tectoast.emolga.utils.BotConstants
import net.dv8tion.jda.api.JDA
import org.koin.core.annotation.Single

@Single
class ProductionJDAProvider(
    private val emolgaJDA: JDA,
    private val flegmonJDA: JDA,
    private val botConstants: BotConstants
) : JDAProvider {
    override fun provideJDA(guild: Long) = if (guild == botConstants.flegmonGuildId) flegmonJDA else emolgaJDA
}