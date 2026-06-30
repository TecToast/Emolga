package de.tectoast.emolga.utils

import de.tectoast.emolga.domain.league.showdownnames.model.ShowdownUserID
import de.tectoast.emolga.domain.pokemon.model.ShowdownID

private val SD_ID_PATTERN = Regex("[^a-zA-Z\\däöüÄÖÜß♂♀é+]+")
private val USERNAME_PATTERN = Regex("[^a-zA-Z\\d]+")
fun String.toShowdownID() = ShowdownID(SD_ID_PATTERN.replace(this.lowercase().replace('é', 'e'), ""))
fun String.toShowdownUserId() = ShowdownUserID(
    USERNAME_PATTERN.replace(
        lowercase().trim().replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss"), ""
    )
)
