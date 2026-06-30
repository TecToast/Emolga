package de.tectoast.emolga.features.system

import de.tectoast.emolga.features.system.types.Feature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single


@Single
class FeatureRegistry(
    val loadListeners: List<ListenerProvider>,
) {
    val featureList = loadListeners.filterIsInstance<Feature<*, *, *>>()
    val registeredFeatureList = featureList.filter { it.spec is RegisteredFeatureSpec }
    val featureNames = registeredFeatureList.map { it.spec.name }
}