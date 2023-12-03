package de.tectoast.emolga.utils.interactive

import net.dv8tion.jda.api.entities.Message

data class Layer(
    val id: String,
    val msg: String,
    val check: (Message, Interactive) -> Any,
    val toString: (Any) -> String
) {

    var answer: Any? = null

    val answerAsString: String
        get() = toString(answer!!)
    val isFinished: Boolean
        get() = answer != null


}
