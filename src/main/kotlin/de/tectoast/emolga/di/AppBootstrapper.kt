package de.tectoast.emolga.di

import org.koin.core.annotation.Single

@Single
class AppBootstrapper(val tasks: List<StartupTask>) {
    suspend fun start() {
        tasks.forEach { it.onStartup() }
    }
}
