package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
sealed class RemoteServerControlConfig {

    @Transient
    open val features: Set<RemoteServerControlFeature> = setOf()

    @Serializable
    @SerialName("Http")
    data class Http(val url: String, val writePin: Int, val readPin: Int) : RemoteServerControlConfig() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START,
            RemoteServerControlFeature.STOP,
            RemoteServerControlFeature.STATUS,
            RemoteServerControlFeature.POWEROFF
        )
    }

    @Serializable
    @SerialName("HomeAssistant")
    data class HomeAssistant(
        val url: String, val webhookIdOn: String, val webhookIdOff: String, val entityId: String, val token: String
    ) : RemoteServerControlConfig() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START, RemoteServerControlFeature.POWEROFF, RemoteServerControlFeature.STATUS
        )

    }

}
