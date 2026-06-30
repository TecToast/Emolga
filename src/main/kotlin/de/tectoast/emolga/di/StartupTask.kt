package de.tectoast.emolga.di

interface StartupTask {
    val priority: Int get() = 0
    suspend fun onStartup()
}

interface DiscordReadyTask {
    suspend fun onDiscordReady()
}