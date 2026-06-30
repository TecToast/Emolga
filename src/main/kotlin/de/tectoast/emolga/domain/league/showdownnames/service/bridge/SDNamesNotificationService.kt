package de.tectoast.emolga.domain.league.showdownnames.service.bridge

import de.tectoast.emolga.domain.league.showdownnames.model.ShowdownUserID

interface SDNamesNotificationService {
    suspend fun sendApprovalNotification(name: String, showdownUserId: ShowdownUserID, id: Long, currentOwner: Long)
}