package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class IPChecker(private val httpClient: HttpClient) : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("ipchecker")) {

    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.deferReply(true)
        iData.replyRaw(httpClient.get("https://api.ipify.org").bodyAsText(), ephemeral = true)
    }
}
