@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.utils.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object EmolgaConfigHelper {
    fun List<Annotation>.findConfig(): Config? {
        return this.find { a -> a is Config } as Config?
    }

    fun buildFromDescriptor(descriptor: SerialDescriptor): JsonObject {
        val config = descriptor.annotations.findConfig()
        return buildJsonObject {
            put("name", config?.name ?: descriptor.serialName)
            put("desc", config?.desc ?: "Keine Beschreibung verfügbar")
            put("type", descriptor.kind.toString())
            if (descriptor.elementsCount > 0)
                put("value", buildJsonObject { build(this, descriptor) })
        }
    }

    fun build(builder: JsonObjectBuilder, descriptor: SerialDescriptor) {
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
                    if (elementDescriptor.kind == StructureKind.CLASS || elementDescriptor.kind == PolymorphicKind.SEALED)
                        put("listtype", elementDescriptor.serialName)
                } else if (desc.kind == StructureKind.MAP) {
                    val elementDescriptor = desc.getElementDescriptor(1)
                    if (elementDescriptor.kind == StructureKind.CLASS)
                        put("maptype", elementDescriptor.serialName)
                }
                if (config == null) put("noconfig", true)
                else {
                    put("name", config.name)
                    put("desc", config.desc)
                    if (config.prio != Int.MAX_VALUE)
                        put("prio", config.prio)
                    config.longType.takeUnless { lt -> lt == LongType.NONE }?.let { lt -> put("longType", lt.name) }
                }
                if (desc.elementsCount > 0)
                    put("value", buildJsonObject {
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

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Config(
    val name: String,
    val desc: String,
    val longType: LongType = LongType.NONE,
    val prio: Int = Int.MAX_VALUE
)

enum class LongType {
    NONE,
    NORMAL,
    MEMBER,
    CHANNEL,
    ROLE
}