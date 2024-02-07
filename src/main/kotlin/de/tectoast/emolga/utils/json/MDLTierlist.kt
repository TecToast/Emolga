package de.tectoast.emolga.utils.json

import de.tectoast.emolga.utils.file
import de.tectoast.emolga.utils.otherJSON

object MDLTierlist {
    val get: Map<String, Map<String, Set<String>>> by lazy {
        otherJSON.decodeFromString(
            "mdltierlist.json".file().readText()
        )
    }


}
