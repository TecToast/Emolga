package de.tectoast.emolga.features.interaction

interface InteractionDataHook {
    suspend fun deleteMessageById(id: Long)
}