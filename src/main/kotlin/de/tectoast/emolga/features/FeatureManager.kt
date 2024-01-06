package de.tectoast.emolga.features

import de.tectoast.emolga.commands.RealInteractionData
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import kotlin.reflect.KClass

class FeatureManager(load: Set<Feature<*, *, *>>) {
    private val eventToName =
        load.associate { it.eventClass to it.eventToName }
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>> = load.groupBy { it.eventClass }
        .mapValues { it.value.associate { k -> k.spec.name to k as Feature<*, *, Arguments> } }

    suspend fun handleEvent(e: GenericInteractionCreateEvent) {
        val kClass = e::class
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
