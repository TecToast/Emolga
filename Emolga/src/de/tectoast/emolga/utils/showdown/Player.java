package de.tectoast.emolga.utils.showdown;

import java.util.ArrayList;

public class Player {

    private final ArrayList<SDPokemon> mons = new ArrayList<>();
    //Hazards, die auf der eigenen Seite liegen
    private SDPokemon spikesBy, tSpikesBy, rocksBy;
    private String nickname;
    private boolean winner = false;


    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public SDPokemon getSpikesBy() {
        return spikesBy;
    }

    public void setSpikesBy(SDPokemon spikesBy) {
        this.spikesBy = spikesBy;
    }

    public SDPokemon gettSpikesBy() {
        return tSpikesBy;
    }

    public void settSpikesBy(SDPokemon tSpikesBy) {
        this.tSpikesBy = tSpikesBy;
    }

    public SDPokemon getRocksBy() {
        return rocksBy;
    }

    public void setRocksBy(SDPokemon rocksBy) {
        this.rocksBy = rocksBy;
    }

    public ArrayList<SDPokemon> getMons() {
        return mons;
    }


    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int indexOfName(String s) {
        for (SDPokemon p : mons) {
            try {
                if (p.getPokemon().equals(s)) return mons.indexOf(p);
            } catch (NullPointerException ignored) {
            }
        }
        return -1;
    }

    public int indexOfNick(String s) {
        //System.out.println(s);
        for (SDPokemon p : mons) {
            try {
                //System.out.println(p.getPokemon() + " ::: " + p.getNickname());
                if (p.getNickname().equals(s)) return mons.indexOf(p);
            } catch (NullPointerException ignored) {
            }
        }
        return -1;
    }

}
