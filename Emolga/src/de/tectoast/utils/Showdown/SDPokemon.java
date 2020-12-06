package de.tectoast.utils.Showdown;


public class SDPokemon {
    private String pokemon, nickname;
    private int kills;
    private SDPokemon statusedBy, bindedBy, cursedBy, seededBy, nightmaredBy, confusedBy, lastDmgBy, perishedBy;
    private boolean dead = false;


    public SDPokemon(String poke) {
        pokemon = poke;
    }


    public String DeadToString() {
        if (dead) return "d";
        else return "a";
    }


    public SDPokemon getSeededBy() {
        return seededBy;
    }

    public void setSeededBy(SDPokemon seededBy) {
        this.seededBy = seededBy;
    }

    public SDPokemon getNightmaredBy() {
        return nightmaredBy;
    }

    public void setNightmaredBy(SDPokemon nightmaredBy) {
        this.nightmaredBy = nightmaredBy;
    }

    public SDPokemon getConfusedBy() {
        return confusedBy;
    }

    public void setConfusedBy(SDPokemon confusedBy) {
        this.confusedBy = confusedBy;
    }

    public SDPokemon getCursedBy() {
        return cursedBy;
    }

    public void setCursedBy(SDPokemon cursedBy) {
        this.cursedBy = cursedBy;
    }

    public SDPokemon getBindedBy() {
        return bindedBy;
    }

    public void setBindedBy(SDPokemon bindedBy) {
        this.bindedBy = bindedBy;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public String getPokemon() {
        return pokemon;
    }

    public void setPokemon(String pokemon) {
        this.pokemon = pokemon;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getKills() {
        return kills;
    }

    public SDPokemon getStatusedBy() {
        return statusedBy;
    }

    public void setStatusedBy(SDPokemon statusedBy) {
        this.statusedBy = statusedBy;
    }

    public SDPokemon getLastDmgBy() {
        return lastDmgBy;
    }

    public void setLastDmgBy(SDPokemon lastDmgBy) {
        this.lastDmgBy = lastDmgBy;
    }

    public void killsPlus1() {
        this.kills++;
    }


    public SDPokemon getPerishedBy() {
        return perishedBy;
    }


    public void setPerishedBy(SDPokemon perishedBy) {
        this.perishedBy = perishedBy;
    }


}
