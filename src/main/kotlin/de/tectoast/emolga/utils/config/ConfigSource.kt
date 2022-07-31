package de.tectoast.emolga.utils.config

import de.tectoast.emolga.utils.config.ConfigSource.ConfigMapper
import de.tectoast.emolga.utils.json.Emolga
import java.util.function.Supplier

enum class ConfigSource(private val configMapper: ConfigMapper) {
    JSON(ConfigMapper { gid: Long, configName: String, key: String, defaultValue: Supplier<Any> ->
        Emolga.get.configuration.getOrPut(gid) { mutableMapOf() }.getOrPut(configName) { mutableMapOf() }[key]
            ?: defaultValue.get()
    });

    operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any {
        return configMapper[gid, configName, key, defaultValue]
    }

    private fun interface ConfigMapper {
        operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any
    }
}