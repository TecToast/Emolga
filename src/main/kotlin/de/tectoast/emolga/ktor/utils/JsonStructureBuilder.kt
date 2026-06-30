@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.ktor.utils

import de.tectoast.emolga.utils.newThreadSafeCache
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.annotation.Single


@Single
class JsonStructureBuilder {

    private val structureCache = newThreadSafeCache<String, JsonObject>(maxSize = 10)

    fun buildFromDescriptor(descriptor: SerialDescriptor, submitString: String? = null): JsonObject {
        structureCache[descriptor.serialName]?.let { return it }
        val config = descriptor.annotations.findConfig()
        return buildJsonObject {
            put("name", config?.name ?: descriptor.serialName)
            put("desc", config?.desc ?: "Keine Beschreibung verfügbar")
            put("type", descriptor.kind.toString())
            submitString?.let { put("submit", it) }
            if (descriptor.elementsCount > 0) put("value", buildJsonObject { build(this, descriptor) })
        }.also {
            structureCache[descriptor.serialName] = it
        }
    }

    private fun build(builder: JsonObjectBuilder, descriptor: SerialDescriptor) {
        if (descriptor.elementsCount == 0) {
            builder.put("type", descriptor.kind.toString())
            builder.put("single", true)
            return
        }
        (0..<descriptor.elementsCount).forEach {
            val desc = descriptor.getElementDescriptor(it)
            val name = descriptor.getElementName(it)
            val annos = descriptor.getElementAnnotations(it)
            val config = annos.findConfig() ?: desc.annotations.findConfig()
            builder.put(name, buildJsonObject {
                put("type", desc.kind.toString())
                if (desc.kind == PolymorphicKind.SEALED) put("sealed", desc.serialName)
                if (desc.isNullable) put("nullable", desc.serialName.substringBefore("?"))
                if (desc.kind == StructureKind.LIST) {
                    val elementDescriptor = desc.getElementDescriptor(0)
                    if (elementDescriptor.kind == StructureKind.CLASS || elementDescriptor.kind == PolymorphicKind.SEALED) put(
                        "listtype", elementDescriptor.serialName
                    )
                } else if (desc.kind == StructureKind.MAP) {
                    val elementDescriptor = desc.getElementDescriptor(1)
                    if (elementDescriptor.kind == StructureKind.CLASS) put("maptype", elementDescriptor.serialName)
                }
                if (config == null) put("noconfig", true)
                else {
                    put("name", config.name)
                    put("desc", config.desc)
                    if (config.prio != Int.MAX_VALUE) put("prio", config.prio)
                    if (config.keyIsDiscordUser) put("keyIsDiscordUser", true)
                    config.longType.takeUnless { lt -> lt == LongType.NONE }?.let { lt -> put("longType", lt.name) }
                }
                if (desc.elementsCount > 0) put("value", buildJsonObject {
                    if (desc.kind == PolymorphicKind.SEALED) {
                        build(this, desc.getElementDescriptor(1))
                    } else {
                        build(this, desc)
                    }
                })
            })
        }
    }
}


