package de.tectoast.emolga.utils.records;

import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.concurrent.CompletableFuture;

public record DeferredSlashResponse(CompletableFuture<InteractionHook> cf) {

    public void reply(String message) {
        cf.thenCompose(ih -> ih.sendMessage(message).submit());
    }
}
