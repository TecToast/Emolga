package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.utils.NotificationFlow
import org.koin.core.annotation.Single

@Single
class PicksModifiedFlow : NotificationFlow<Long>()