package de.tectoast.emolga.domain.util.service

import de.tectoast.emolga.ktor.utils.findConfig
import de.tectoast.emolga.utils.newThreadSafeCache
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class DefaultDataService(@Named("web") private val webJson: Json) {
    private val defaultDataCache = newThreadSafeCache<String, String>(maxSize = 100)

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    fun getDefaultData(path: String): String? {
        if (!path.startsWith("de.tectoast")) return null
        defaultDataCache[path]?.let { return it }
        val split = path.split("#")
        var parentSerializer: KSerializer<Any>? = null
        val serializer = runCatching {
            if (split.size == 2) Class.forName(split[0]).kotlin.also {
                parentSerializer = it.serializerOrNull() as KSerializer<Any>?
            }.sealedSubclasses.mapNotNull { it.serializerOrNull() }
                .sortedBy { it.descriptor.annotations.findConfig()?.prio ?: Int.MAX_VALUE }
                .first { split[1] == "" || it.descriptor.serialName == split[1] } else Class.forName(path).kotlin.serializerOrNull()
        }.getOrNull() as? KSerializer<Any>? ?: return null
        val value =
            runCatching { webJson.decodeFromString(serializer, "{}") }.getOrNull() ?: return null
        val defaultData = webJson.encodeToString(parentSerializer ?: serializer, value)
        defaultDataCache[path] = defaultData
        return defaultData
    }
}
