package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*

object IPChecker : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("ipchecker")) {

    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.deferReply(true)
        iData.reply(httpClient.get("https://api.ipify.org").bodyAsText(), ephemeral = true)
    }
}
