package de.tectoast.emolga.utils;

import de.tectoast.emolga.utils.showdown.Player;

import java.util.HashMap;
import java.util.List;

@FunctionalInterface
public interface ReplayAnalyser {
    void analyse(Player[] game, String uid1, String uid2, List<HashMap<String, String>> kills, List<HashMap<String, String>> deaths, Object... optionalArgs);
}
