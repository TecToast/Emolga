package de.tectoast.emolga.features.system.model

import de.tectoast.emolga.features.interaction.InteractionData

typealias AllowedResultCheck = suspend InteractionData.() -> AllowedResult