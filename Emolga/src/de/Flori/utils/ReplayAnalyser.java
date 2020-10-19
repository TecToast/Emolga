package de.Flori.utils;

import de.Flori.utils.Showdown.Player;

import java.util.HashMap;

@FunctionalInterface
public interface ReplayAnalyser {
    void analyse(Player[] game, String uid1, String uid2, HashMap<String, String> kills, HashMap<String, String> deaths);
}
