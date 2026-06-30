package de.tectoast.emolga.domain.discord.service

interface GeneralDiscordService {
    suspend fun updatePresence()
    suspend fun enableMaintenance(reason: String)
    suspend fun disableMaintenance()

    companion object {
        const val ROUTINE_MAINTENANCE_KEY = "ROUTINE"
    }
}