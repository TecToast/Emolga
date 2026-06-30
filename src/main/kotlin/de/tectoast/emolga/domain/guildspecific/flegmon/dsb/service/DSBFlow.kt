package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBMessage
import de.tectoast.emolga.utils.NotificationFlow
import org.koin.core.annotation.Single

@Single
class DSBFlow : NotificationFlow<DSBMessage>()