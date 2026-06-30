package de.tectoast.emolga.features.system

import de.tectoast.k18n.generated.K18nMessage

sealed class FeatureSpec(open val name: String, val aliases: MutableSet<String> = mutableSetOf())
sealed class RegisteredFeatureSpec(name: String) : FeatureSpec(name) {
    var inDM = false
}

class CommandSpec(name: String, val help: K18nMessage) : RegisteredFeatureSpec(name)
class ButtonSpec(name: String) : FeatureSpec(name)
class ModalSpec(name: String) : FeatureSpec(name)
class SelectMenuSpec(name: String) : FeatureSpec(name)
class MessageContextSpec(name: String) : RegisteredFeatureSpec(name)