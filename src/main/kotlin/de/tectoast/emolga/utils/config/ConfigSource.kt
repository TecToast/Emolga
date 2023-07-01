package de.tectoast.emolga.utils.config

import de.tectoast.emolga.utils.config.ConfigSource.ConfigMapper
import de.tectoast.emolga.utils.json.Configuration
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import java.util.function.Supplier

enum class ConfigSource(private val configMapper: ConfigMapper) {
    JSON(ConfigMapper { gid: Long, configName: String, key: String, defaultValue: Supplier<Any> ->
        runBlocking {
            db.configuration.findOne(Configuration::guild eq gid)?.data?.get(
                configName
            )?.get(key) ?: defaultValue.get()
        }
    });

    operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any {
        return configMapper[gid, configName, key, defaultValue]
    }

    private fun interface ConfigMapper {
        operator fun get(gid: Long, configName: String, key: String, defaultValue: Supplier<Any>): Any
    }
}
