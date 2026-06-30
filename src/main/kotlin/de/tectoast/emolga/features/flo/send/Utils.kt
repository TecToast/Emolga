package de.tectoast.emolga.features.flo.send

import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.utils.k18n

class SendArgs : Arguments() {
    var id by long("id", "id".k18n)
    var msg by string("msg", "msg".k18n)
}

fun String.convertForSend() = this.replace("\\n", "\n")