package de.tectoast.emolga

import de.tectoast.emolga.di.*
import mu.KotlinLogging
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin


private val logger = KotlinLogging.logger {}

@Module(includes = [ConfigModule::class, DatabaseModule::class, DiscordModule::class, PlatformModule::class, JsonModule::class])
@ComponentScan("de.tectoast.emolga")
class ProductionAppModule

@KoinApplication(modules = [ProductionAppModule::class])
object ProductionApp

suspend fun main() {
    val koinApplication = startKoin<ProductionApp>()
    koinApplication.koin.get<AppBootstrapper>().start()
}
