package de.tectoast.emolga.utils.records

import de.tectoast.emolga.utils.automation.structure.ModalConfigurator
import java.util.function.Supplier

class ModalConfiguration(val name: String, private val configuratorSupplier: Supplier<ModalConfigurator>) {
    fun configurator(): ModalConfigurator {
        return configuratorSupplier.get()
    }
}