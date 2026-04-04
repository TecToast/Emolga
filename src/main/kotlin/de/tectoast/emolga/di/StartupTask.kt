package de.tectoast.emolga.di

import net.dv8tion.jda.api.JDA

interface StartupTask {
    suspend fun onStartup()
}

interface JDAReadyTask {
    suspend fun onJDAReady(jda: JDA)
}