package de.tectoast.emolga.features

import de.tectoast.emolga.commands.RealInteractionData
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import kotlin.reflect.KClass

class FeatureManager(load: Set<Feature<*, *, *>>) {
    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String> =
        load.associate { it.eventClass to it.eventToName }
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>> = load.groupBy { it.eventClass }
        .mapValues { it.value.associate { k -> k.spec.name to k as Feature<*, GenericInteractionCreateEvent, Arguments> } }
    private val listeners = load.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
        it.value.map { v -> v.second }
    }

    suspend fun handleEvent(e: GenericInteractionCreateEvent) {
        val kClass = e::class
        listeners[kClass]?.forEach { it(e) }
        val eventFeatures = features[kClass] ?: return
        val feature = eventFeatures[eventToName[kClass]!!(e)]!!
        val args = feature.argsFun()
        val data = RealInteractionData(e)
        feature.populateArgs(data, e, args)
        with(data) {
            feature.exec(args)
        }
    }

}

fun <T, K, V> Iterable<T>.groupByListAndMap(keySelector: (T) -> Collection<K>, mapper: (T) -> V): Map<K, List<V>> {
    val map = mutableMapOf<K, MutableList<V>>()
    for (element in this) {
        for (key in keySelector(element)) {
            val list = map.getOrPut(key) { mutableListOf() }
            list.add(mapper(element))
        }
    }
    return map
}
