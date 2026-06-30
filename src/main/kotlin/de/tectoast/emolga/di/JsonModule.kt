package de.tectoast.emolga.di

import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class JsonModule {

    @Single
    @Named("web")
    fun webJson() = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(Long::class, LongAsStringSerializer)
        }
    }

    @Single
    @Named("strictWeb")
    fun strictWebJson(): Json {
        return Json {
            ignoreUnknownKeys = false
            isLenient = false
            serializersModule = SerializersModule {
                contextual(Long::class, LongAsStringSerializer)
            }
        }
    }
}