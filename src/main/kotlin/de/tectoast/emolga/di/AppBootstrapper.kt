package de.tectoast.emolga.di

import org.koin.core.annotation.Single

@Single
class AppBootstrapper(private val tasks: List<StartupTask>) {
    suspend fun start() {
        tasks.sortedByDescending { it.priority }.forEach { it.onStartup() }
    }
}
