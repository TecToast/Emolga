package de.tectoast.emolga.features.system.model

import de.tectoast.emolga.features.system.Arg
import de.tectoast.emolga.utils.default
import de.tectoast.generic.K18n_MissingArgument
import de.tectoast.k18n.generated.K18nMessage

open class ArgumentException(open val msg: K18nMessage) : Exception(msg.default())

class MissingArgumentException(arg: Arg<*, *>) : ArgumentException(K18n_MissingArgument(arg.name))

class InvalidArgumentException(msg: K18nMessage) : ArgumentException(msg)
