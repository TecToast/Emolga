package de.tectoast.emolga.utils.json

import de.tectoast.emolga.commands.Command.Companion.load

object MDLTierlist {
    //val get: Map<String, Map<String, Set<String>>> by lazy { load("mdltierlist.json") }
    val get: Map<String, Map<String, Set<String>>> get() = load("mdltierlist.json")


}
