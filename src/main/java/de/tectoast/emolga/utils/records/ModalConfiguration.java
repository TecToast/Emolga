package de.tectoast.emolga.utils.records;

import de.tectoast.emolga.utils.automation.structure.ModalConfigurator;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public record ModalConfiguration(String name, Supplier<ModalConfigurator> configuratorSupplier) {
    public ModalConfigurator configurator() {
        return configuratorSupplier.get();
    }
}