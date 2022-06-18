package de.tectoast.emolga.utils.config;

import de.tectoast.emolga.commands.Command;
import de.tectoast.jsolf.JSONObject;

import java.util.function.Supplier;

public enum ConfigSource {
    JSON((gid, configName, key, defaultValue) -> {
        JSONObject conf = Command.getEmolgaJSON().createOrGetJSON("configuration").createOrGetJSON(gid).createOrGetJSON(configName);
        return conf.has(key) ? conf.get(key) : defaultValue.get();
    });

    private final ConfigMapper configMapper;

    ConfigSource(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public Object get(long gid, String configName, String key, Supplier<Object> defaultValue) {
        return configMapper.get(gid, configName, key, defaultValue);
    }

    @FunctionalInterface
    private interface ConfigMapper {
        Object get(long gid, String configName, String key, Supplier<Object> defaultValue);
    }
}
