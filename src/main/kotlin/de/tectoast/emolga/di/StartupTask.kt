package de.tectoast.emolga.di

interface StartupTask {
    suspend fun onStartup()
}
