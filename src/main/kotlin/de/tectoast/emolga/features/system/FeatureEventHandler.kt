package de.tectoast.emolga.features.system

import net.dv8tion.jda.api.events.GenericEvent

interface FeatureEventHandler {
    suspend fun handleEvent(e: GenericEvent)
}