package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.httpClient
import de.tectoast.emolga.utils.jsonb
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object FlorixControlTable : Table("florixcontrol") {
    val name = varchar("name", 100)
    val data = jsonb<RemoteServerControlData>("data")
}

class FlorixControlRepository(val db: R2dbcDatabase) {
    suspend fun getByName(name: String) = suspendTransaction(db) {
        FlorixControlTable.select(FlorixControlTable.data)
            .where { FlorixControlTable.name eq name }
            .firstOrNull()
            ?.let { it[FlorixControlTable.data] }
    }
}

enum class RemoteServerControlFeature {
    START, STATUS, STOP, POWEROFF
}

@Serializable
sealed class RemoteServerControlData {

    @Transient
    open val features: Set<RemoteServerControlFeature> = setOf()

    open suspend fun startServer() {}
    open suspend fun isOn(): Boolean = false
    open suspend fun stopServer() {}
    open suspend fun powerOff() {}

    @Serializable
    @SerialName("Http")
    data class Http(val url: String, val writePin: Int, val readPin: Int) : RemoteServerControlData() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START,
            RemoteServerControlFeature.STOP,
            RemoteServerControlFeature.STATUS,
            RemoteServerControlFeature.POWEROFF
        )

        override suspend fun startServer() = push(TURN_ON_TIME)

        override suspend fun stopServer() = push(TURN_OFF_TIME)

        override suspend fun powerOff() = push(POWER_OFF)

        private suspend fun push(delay: Int) {
            withContext(Dispatchers.IO) {
                httpClient.post("$url/push/$writePin") {
                    setBody("$delay")
                }
            }
        }

        override suspend fun isOn() = withContext(Dispatchers.IO) {
            httpClient.get("$url/status/$readPin").bodyAsText().contains("level=0")
        }

        companion object {
            private const val TURN_ON_TIME = 500
            private const val TURN_OFF_TIME = 500
            private const val POWER_OFF = 5000
        }
    }

    @Serializable
    @SerialName("HomeAssistant")
    data class HomeAssistant(
        val url: String, val webhookIdOn: String, val webhookIdOff: String, val entityId: String, val token: String
    ) : RemoteServerControlData() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START, RemoteServerControlFeature.POWEROFF, RemoteServerControlFeature.STATUS
        )

        override suspend fun startServer(): Unit = withContext(Dispatchers.IO) {
            println(httpClient.post("http://$url/api/webhook/$webhookIdOn").bodyAsText())
        }

        override suspend fun powerOff(): Unit = withContext(Dispatchers.IO) {
            println(httpClient.post("http://$url/api/webhook/$webhookIdOff").bodyAsText())
        }

        override suspend fun isOn(): Boolean {
            return when (val res = httpClient.get("http://$url/api/states/$entityId") {
                bearerAuth(token)
            }.body<HAResponseData>().state) {
                "on" -> true
                "off" -> false
                else -> error("Unknown HA response $res")
            }
        }

        @Serializable
        data class HAResponseData(val state: String)
    }

}