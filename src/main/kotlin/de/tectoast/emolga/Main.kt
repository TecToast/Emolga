package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.di.ConfigModule
import de.tectoast.emolga.di.DatabaseModule
import de.tectoast.emolga.di.DiscordModule
import de.tectoast.emolga.di.PlatformModule
import de.tectoast.emolga.ktor.Ktor
import de.tectoast.emolga.utils.defaultScope
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.initMongo
import de.tectoast.emolga.utils.repeat.IntervalTask
import de.tectoast.emolga.utils.repeat.RepeatTask
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin


private val logger = KotlinLogging.logger {}

@Module(includes = [ConfigModule::class, DatabaseModule::class, DiscordModule::class, PlatformModule::class])
@ComponentScan("de.tectoast.emolga")
class ProductionAppModule

@KoinApplication(modules = [ProductionAppModule::class])
object ProductionApp

suspend fun main() {
    startKoin<ProductionApp>()
    logger.info("Starting Bot...")
    logger.info("Starting MongoDB...")
    initMongo()
    logger.info("Launching Bots...")
    EmolgaMain.launchBots()
    Tierlist.setup()
    logger.info("Starting DB...")
    defaultScope.launch {
        RepeatTask.setupRepeatTasks()
        IntervalTask.setupIntervalTasks()
    }
    logger.info("Starting KTor...")
    Ktor.start()
    logger.info("Starting EmolgaMain...")
    EmolgaMain.startListeners()
    TODO("AppBootstrapper")
}
