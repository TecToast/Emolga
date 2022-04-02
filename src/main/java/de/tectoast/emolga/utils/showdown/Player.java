package de.tectoast.emolga.utils.showdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private Pokemon spikesBy, tSpikesBy, rocksBy;

    private ArrayList<Pokemon> mons = new ArrayList<>();
    private String nickname;
    private boolean winner = false;
    private int totalKills = 0;
    private int totalDeaths = 0;
    private int teamsize;
    private final int number;

    public Player(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public int getTeamsize() {
        return teamsize;
    }

    public void setTeamsize(int teamsize) {
        this.teamsize = teamsize;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public Pokemon getSpikesBy(Pokemon other) {
        if (spikesBy == null) return null;
        if (spikesBy.getPlayer() == this) return other;
        return spikesBy;
    }

    public void setSpikesBy(Pokemon spikesBy) {
        this.spikesBy = spikesBy;
    }

    public Pokemon gettSpikesBy(Pokemon other) {
        if (tSpikesBy == null) return null;
        if (tSpikesBy.getPlayer() == this) return other;
        return tSpikesBy;
    }

    public void settSpikesBy(Pokemon tSpikesBy) {
        this.tSpikesBy = tSpikesBy;
    }

    public Pokemon getRocksBy(Pokemon other) {
        if (rocksBy == null) return null;
        if (rocksBy.getPlayer() == this) return other;
        return rocksBy;
    }

    public void setRocksBy(Pokemon rocksBy) {
        this.rocksBy = rocksBy;
    }

    public ArrayList<Pokemon> getMons() {
        return mons;
    }

    public void setMons(ArrayList<Pokemon> mons) {
        this.mons = mons;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int indexOfName(String s) {
        for (Pokemon p : mons) {
            try {
                if (p.getPokemon().equals(s)) return mons.indexOf(p);
            } catch (NullPointerException ignored) {
            }
        }
        return -1;
    }

    public int getDisplayNumber() {
        return mons.size() - totalDeaths;
    }

    public void addTotalKills(int totalKills) {
        this.totalKills += totalKills;
    }

    public void addTotalDeaths(int totalDeaths) {
        this.totalDeaths += totalDeaths;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    @Override
    public String toString() {
        return "Player{nickname='" + nickname + "'}";
    }
}
