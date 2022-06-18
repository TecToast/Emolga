package de.tectoast.emolga.utils;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.dexquiz.DexQuizTip;
import de.tectoast.emolga.utils.config.ConfigSource;

import java.util.function.Function;

public enum ConfigManager {
    DEXQUIZ(s -> {
        if (s.equals("totalbudget")) return Command.DEXQUIZ_BUDGET;
        return DexQuizTip.valueOf(s).getDefaultPrice();
    });

    private final Function<String, Object> defaultValue;
    private final ConfigSource configSource;

    ConfigManager(Function<String, Object> defaultValue) {
        this(defaultValue, ConfigSource.JSON);
    }

    ConfigManager(Function<String, Object> defaultValue, ConfigSource configSource) {
        this.defaultValue = defaultValue;
        this.configSource = configSource;
    }

    public Object getValue(long gid, String key) {
        return configSource.get(gid, name().toLowerCase(), key, () -> defaultValue.apply(key));
    }
}
