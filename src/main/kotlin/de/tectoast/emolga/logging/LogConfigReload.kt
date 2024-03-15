package de.tectoast.emolga.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil
import ch.qos.logback.core.spi.ConfigurationEvent
import ch.qos.logback.core.status.StatusUtil
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

object LogConfigReload {
    private val scope = createCoroutineScope("LogConfigReload")
    private val logger = KotlinLogging.logger {}
    private var lastReload = System.currentTimeMillis()

    fun start() {
        scope.launch {
            val context = LoggerFactory.getILoggerFactory() as LoggerContext
            val watchService = FileSystems.getDefault().newWatchService()
            val mainConfigurationURL = ConfigurationWatchListUtil.getMainWatchURL(context)
            logger.info("Watching $mainConfigurationURL for changes")
            val configPath = Path.of(mainConfigurationURL.path)
            val parentPath = configPath.parent
            parentPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
            while (true) {
                val monitorKey = watchService.take()
                val dirPath = monitorKey.watchable() as Path
                monitorKey.pollEvents().forEach {
                    val eventPath = dirPath.resolve(it.context() as Path)
                    if (eventPath != configPath) {
                        return@forEach
                    }
                    val ctm = System.currentTimeMillis()
                    if (ctm - lastReload < 1000) {
                        return@forEach
                    }
                    lastReload = ctm
                    logger.info("Reloading configuration...")
                    context.fireConfigurationEvent(ConfigurationEvent.newConfigurationChangeDetectedEvent(this))
                    performXMLConfiguration(context, mainConfigurationURL)
                    logger.info("Configuration reloaded")
                }
                monitorKey.reset()
            }
        }
    }

    private fun performXMLConfiguration(lc: LoggerContext, mainConfigurationURL: URL) {
        val jc = JoranConfigurator()
        jc.context = lc
        val statusUtil = StatusUtil(lc)
        lc.reset()
        val threshold = System.currentTimeMillis()
        try {
            jc.doConfigure(mainConfigurationURL)
            // e.g. IncludeAction will add a status regarding XML parsing errors but no exception will reach here
            if (statusUtil.hasXMLParsingErrors(threshold)) {
                logger.error { "XML parsing errors in configuration files." }
            }
        } catch (e: JoranException) {
            logger.error(e) { "Error occurred while configuring logback" }
        }
    }

}
