package de.tectoast.emolga.domain.league.doc.service

import de.tectoast.emolga.domain.league.doc.model.HideGamesInsertData
import de.tectoast.emolga.utils.NotificationFlow
import org.koin.core.annotation.Single

@Single
class HideGamesInsertFlow : NotificationFlow<HideGamesInsertData>()