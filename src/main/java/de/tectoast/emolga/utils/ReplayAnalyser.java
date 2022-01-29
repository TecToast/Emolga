package de.tectoast.emolga.utils;

import de.tectoast.emolga.utils.showdown.Player;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ReplayAnalyser {
    void analyse(Player[] game, String uid1, String uid2, List<Map<String, String>> kills, List<Map<String, String>> deaths, Object... optionalArgs);
}
