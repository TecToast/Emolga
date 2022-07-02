package de.tectoast.emolga.utils.records

import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.concurrent.CompletableFuture

class DeferredSlashResponse(private val cf: CompletableFuture<InteractionHook>) {
    fun reply(message: String) {
        cf.thenCompose { ih: InteractionHook ->
            ih.sendMessage(
                message
            ).submit()
        }
    }
}