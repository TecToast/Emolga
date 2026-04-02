package de.tectoast.emolga.utils

interface MessageSender {
    suspend fun sendMessage(message: String)
}
