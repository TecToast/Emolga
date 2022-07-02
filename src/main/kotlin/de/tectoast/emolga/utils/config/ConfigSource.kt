package de.tectoast.emolga.utils.config

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.config.ConfigSource.ConfigMapper
import java.util.function.Supplier

enum class ConfigSource(private val configMapper: ConfigMapper) {
    JSON(ConfigMapper { gid: Long, configName: String, key: String, defaultValue: Supplier<Any> ->
        val conf = Command.emolgaJSON.createOrGetJSON("configuration").createOrGetJSON(gid).createOrGetJSON(configName)
        if (conf.has(key)) conf[key] else defaultValue.get()
    });

    operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any {
        return configMapper[gid, configName, key, defaultValue]
    }

    private fun interface ConfigMapper {
        operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any
    }
}