package de.tectoast.emolga.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil
import ch.qos.logback.core.spi.ConfigurationEvent
import ch.qos.logback.core.status.StatusUtil
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.net.URL

object LogConfigReload {
    private val logger = KotlinLogging.logger {}

    fun reloadConfiguration() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val mainConfigurationURL = ConfigurationWatchListUtil.getMainWatchURL(context)
        logger.info("Reloading configuration...")
        context.fireConfigurationEvent(ConfigurationEvent.newConfigurationChangeDetectedEvent(this))
        performXMLConfiguration(context, mainConfigurationURL)
        logger.info("Configuration reloaded")
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

