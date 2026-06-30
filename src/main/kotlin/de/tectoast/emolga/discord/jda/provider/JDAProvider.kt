package de.tectoast.emolga.discord.jda.provider

import net.dv8tion.jda.api.JDA

interface JDAProvider {
    fun provideJDA(guild: Long): JDA
}
