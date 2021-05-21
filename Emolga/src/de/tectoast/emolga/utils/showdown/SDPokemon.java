package de.tectoast.emolga.utils.showdown;


public class SDPokemon {
    private String pokemon, nickname;
    private int kills;
    private SDPokemon statusedBy, bindedBy, cursedBy, seededBy, nightmaredBy, confusedBy, lastDmgBy, perishedBy, orbBy;
    private boolean dead = false;
    private String origMon;


    public SDPokemon(String poke) {
        pokemon = poke;
    }


    public String DeadToString() {
        if (dead) return "d";
        else return "a";
    }

    public void setOrigMon(String origMon) {
        this.origMon = origMon;
    }

    public boolean isMon(String name) {
        //System.out.println(name + " ======> " + pokemon + " ||| " + origMon);
        return name.equals(pokemon) || name.equals(origMon);
    }

    public SDPokemon getOrbBy() {
        return orbBy;
    }

    public void setOrbBy(SDPokemon orbBy) {
        this.orbBy = orbBy;
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

    @Override
    public String toString() {
        return "SDPokemon{" + "pokemon='" + pokemon + '\'' +
                ", nickname='" + nickname + '\'' +
                ", kills=" + kills +
                ", statusedBy=" + statusedBy +
                ", bindedBy=" + bindedBy +
                ", cursedBy=" + cursedBy +
                ", seededBy=" + seededBy +
                ", nightmaredBy=" + nightmaredBy +
                ", confusedBy=" + confusedBy +
                ", lastDmgBy=" + lastDmgBy +
                ", perishedBy=" + perishedBy +
                ", dead=" + dead +
                '}';
    }
}
