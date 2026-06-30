package de.tectoast.emolga.domain.league.showdownnames.service

import de.tectoast.emolga.domain.league.showdownnames.model.SDInsertStatus
import de.tectoast.emolga.domain.league.showdownnames.repository.SDNamesRepository
import de.tectoast.emolga.domain.league.showdownnames.service.bridge.SDNamesNotificationService
import de.tectoast.emolga.utils.toShowdownUserId
import org.koin.core.annotation.Single

@Single
class ShowdownNameInsertService(
    private val repo: SDNamesRepository,
    private val notificationService: SDNamesNotificationService
) {
    /**
     * Adds a showdown name/id combination to the database (if the name is not already used)
     * @param name the showdown name
     * @param id the discord user id
     * @return [SDInsertStatus] showing the result of the operation
     */
    suspend fun addIfAbsent(name: String, id: Long): SDInsertStatus {
        val showdownUserId = name.toShowdownUserId()
        val currentOwner = repo.getCurrentOwner(showdownUserId)
        if (currentOwner == null) {
            repo.tryInsertName(showdownUserId, id)
            return SDInsertStatus.SUCCESS
        }
        return if (currentOwner == id) {
            SDInsertStatus.ALREADY_OWNED_BY_YOU
        } else {
            notificationService.sendApprovalNotification(name, showdownUserId, id, currentOwner)
            SDInsertStatus.ALREADY_OWNED_BY_OTHER
        }
    }
}