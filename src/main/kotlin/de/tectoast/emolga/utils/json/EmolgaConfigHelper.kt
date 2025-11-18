@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.utils.json

import com.mongodb.client.model.Updates
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.FormatLanguage
import org.bson.conversions.Bson

object EmolgaConfigHelper {
    fun List<Annotation>.findConfig(): Config? {
        return this.find { a -> a is Config } as Config?
    }

    fun buildFromDescriptor(descriptor: SerialDescriptor, delta: Boolean, submitString: String? = null): JsonObject {
        // TODO: Add caching?
        val config = descriptor.annotations.findConfig()
        return buildJsonObject {
            put("name", config?.name ?: descriptor.serialName)
            put("desc", config?.desc ?: "Keine Beschreibung verfügbar")
            put("type", descriptor.kind.toString())
            if (delta) {
                put("delta", true)
            }
            submitString?.let { put("submit", it) }
            if (descriptor.elementsCount > 0) put("value", buildJsonObject { build(this, descriptor) })
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

    @OptIn(InternalSerializationApi::class)
    fun parseRemoteDelta(
        rootDescriptor: SerialDescriptor, @FormatLanguage("json", "", "") jsonObject: JsonObject
    ): Bson? {
        val parsedDeltas = try {
            flattenValidateAndConvert(jsonObject, rootDescriptor)
        } catch (e: InvalidDeltaValueException) {
            return null
        }
        return Updates.combine(parsedDeltas.map { (key, value) -> Updates.set(key, value) })
    }

    fun JsonElement.toNativeType(): Any? {
        return when (this) {
            is JsonNull -> null
            is JsonPrimitive -> when {
                isString -> content
                booleanOrNull != null -> booleanOrNull
                longOrNull != null -> longOrNull // Wichtig: Long vor Double prüfen
                doubleOrNull != null -> doubleOrNull
                else -> content // Fallback
            }
            // Wichtig: Arrays und Objekte werden als Ganzes zurückgegeben
            is JsonArray -> this.map { it.toNativeType() }
            is JsonObject -> this.mapValues { it.value.toNativeType() }
        }
    }

    fun flattenValidateAndConvert(
        jsonObj: JsonObject, rootDescriptor: SerialDescriptor
    ): Map<String, Any?> {
        val flattenedMap = mutableMapOf<String, Any?>()
        fun recurse(
            element: JsonElement, descriptor: SerialDescriptor, currentPath: String, config: Config? = null
        ) {

            when (element) {
                is JsonPrimitive, is JsonArray -> {
                    val convertedValue = element.toNativeType() // "Dumm" konvertieren
                    val actualValue = getActualValue(convertedValue, descriptor, config)
                    flattenedMap[currentPath] = actualValue
                }

                is JsonObject -> {
                    when (descriptor.kind) {
                        StructureKind.CLASS, StructureKind.OBJECT -> {
                            // Delta für ein normales Objekt
                            element.entries.forEach { (key, childElement) ->
                                val childIndex = descriptor.getElementIndex(key)
                                if (childIndex == -1) {
                                    invalidDelta()
                                }
                                val childDesc = descriptor.getElementDescriptor(childIndex)
                                recurse(
                                    childElement,
                                    childDesc,
                                    "$currentPath.$key",
                                    descriptor.getElementAnnotations(childIndex).findConfig()
                                )
                            }
                        }

                        PolymorphicKind.SEALED -> {
                            val type = element["type"]?.jsonPrimitive?.content ?: invalidDelta()
                            val sealedValueDesc = descriptor.getElementDescriptor(1)
                            val desc = (0..<descriptor.elementsCount).firstOrNull { idx ->
                                sealedValueDesc.getElementName(idx) == type
                            }?.let { sealedValueDesc.getElementDescriptor(it) } ?: invalidDelta()
                            recurse(buildJsonObject {
                                element.entries.forEach { (k, v) ->
                                    if (k != "type") put(k, v)
                                }
                            }, desc, currentPath, config)
                        }

                        StructureKind.LIST -> {
                            // Delta für ein Array (z.B. {"2": {...}})
                            val listElementDesc = descriptor.getElementDescriptor(0) // Typ der Listenelemente
                            element.entries.forEach { (key, childElement) ->
                                if (key.toIntOrNull() == null) {
                                    invalidDelta()
                                }
                                // Rekursion für das Element *innerhalb* der Liste
                                recurse(
                                    childElement,
                                    listElementDesc,
                                    "$currentPath.$key",
                                    descriptor.getElementAnnotations(0).findConfig()
                                )
                            }
                        }

                        StructureKind.MAP -> {
                            val mapValueDesc = descriptor.getElementDescriptor(1) // Typ der Map-Werte
                            element.entries.forEach { (key, childElement) ->
                                recurse(
                                    childElement,
                                    mapValueDesc,
                                    "$currentPath.$key",
                                    descriptor.getElementAnnotations(1).findConfig()
                                )
                            }
                        }

                        else -> throw IllegalStateException("Unerwartetes JsonObject für Descriptor ${descriptor.kind} bei '$currentPath'")
                    }
                }
            }
        }

        jsonObj.entries.forEach { (key, element) ->
            val elementIndex = rootDescriptor.getElementIndex(key)
            if (elementIndex == -1) {
                invalidDelta()
            }
            val elementDesc = rootDescriptor.getElementDescriptor(elementIndex)
            recurse(element, elementDesc, key, rootDescriptor.getElementAnnotations(elementIndex).findConfig())
        }

        return flattenedMap
    }

    class InvalidDeltaValueException : Exception()

    private fun invalidDelta(): Nothing = throw InvalidDeltaValueException()

    private fun getActualValue(value: Any?, descriptor: SerialDescriptor, config: Config?): Any? {
        if (value == null) {
            return if (descriptor.isNullable) value else invalidDelta()
        }
        return when (descriptor.kind) {
            SerialKind.CONTEXTUAL -> {
                if (value !is String) invalidDelta()
                else when (config?.longType) {
                    LongType.CHANNEL -> if (DiscordEntityValidator.validateChannelId(value)) value.toLong() else invalidDelta()
                    LongType.ROLE -> if (DiscordEntityValidator.validateRoleId(value)) value.toLong() else invalidDelta()
                    else -> value
                }
            }

            StructureKind.LIST -> {
                if (value !is List<*>) invalidDelta()
                value.map { item ->
                    getActualValue(
                        item,
                        descriptor.getElementDescriptor(0),
                        config = descriptor.getElementAnnotations(0).findConfig()
                    )
                }
            }

            StructureKind.MAP -> {
                if (value !is Map<*, *>) invalidDelta()
                value.entries.associate { en ->
                    getActualValue(
                        en.key,
                        descriptor.getElementDescriptor(0),
                        config = descriptor.getElementAnnotations(0).findConfig()
                    ) to getActualValue(
                        en.value,
                        descriptor.getElementDescriptor(1),
                        config = descriptor.getElementAnnotations(1).findConfig()
                    )
                }
            }

            StructureKind.CLASS, StructureKind.OBJECT -> {
                if (value !is Map<*, *>) invalidDelta()
                value.entries.associate { (k, v) ->
                    if (k !is String) invalidDelta()
                    val elementIndex = descriptor.getElementIndex(k)
                    if (elementIndex == -1) invalidDelta()
                    k to getActualValue(
                        v,
                        descriptor.getElementDescriptor(elementIndex),
                        config = descriptor.getElementAnnotations(elementIndex).findConfig()
                    )
                }
            }

            else -> {
                if (when (descriptor.kind) {
                        PrimitiveKind.STRING -> value is String
                        PrimitiveKind.INT -> value is Int || value is Long
                        PrimitiveKind.LONG -> value is Long || value is Int
                        PrimitiveKind.BOOLEAN -> value is Boolean
                        PrimitiveKind.DOUBLE -> value is Double || value is Float
                        PrimitiveKind.FLOAT -> value is Float || value is Double

                        SerialKind.ENUM -> value is String


                        else -> true
                    }
                ) value else invalidDelta()
            }
        }
    }
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Config(
    val name: String,
    val desc: String,
    val longType: LongType = LongType.NONE,
    val prio: Int = Int.MAX_VALUE,
    val keyIsDiscordUser: Boolean = false,
)

enum class LongType {
    NONE, NORMAL, MEMBER, CHANNEL, ROLE
}