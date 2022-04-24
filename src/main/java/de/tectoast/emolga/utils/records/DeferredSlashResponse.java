package de.tectoast.emolga.utils.records;

import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public record DeferredSlashResponse(CompletableFuture<InteractionHook> cf) {

    private static final Logger logger = LoggerFactory.getLogger(DeferredSlashResponse.class);

    public void reply(String message) {
        cf.thenCompose(ih -> ih.sendMessage(message).submit());
    }
}
