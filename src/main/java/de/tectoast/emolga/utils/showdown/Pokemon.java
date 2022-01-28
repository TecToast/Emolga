package de.tectoast.emolga.utils.showdown;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

public class Pokemon {
    private static final Logger logger = LoggerFactory.getLogger(Pokemon.class);
    private final Player player;
    private final List<Integer> zoroTurns;
    private final List<String> game;
    private final Supplier<String> disabledAbi;
    private String pokemon;
    private int kills;
    private Pokemon statusedBy, bindedBy, cursedBy, seededBy, nightmaredBy, confusedBy, lastDmgBy, perishedBy;
    private boolean dead = false;
    private int hp = 100;
    private String ability = "";
    private int lastKillTurn = -1;


    public Pokemon(String poke, Player player, List<Integer> zoroTurns, List<String> game, Supplier<String> disabledAbi) {
        pokemon = poke;
        this.zoroTurns = zoroTurns;
        this.game = game;
        this.player = player;
        this.disabledAbi = disabledAbi;
    }

    public Player getPlayer() {
        return player;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public boolean noAbilityTrigger(int line) {
        return !this.ability.isEmpty() && !this.disabledAbi.get().equals(this.ability) && !game.get(line + 1).contains("[from] ability: " + this.ability);
    }

    public boolean checkHPZoro(int hp) {
        return this.hp != hp;
    }

    public Pokemon getSeededBy() {
        return seededBy;
    }

    public void setSeededBy(Pokemon seededBy) {
        this.seededBy = seededBy;
    }

    public Pokemon getNightmaredBy() {
        return nightmaredBy;
    }

    public void setNightmaredBy(Pokemon nightmaredBy) {
        this.nightmaredBy = nightmaredBy;
    }

    public Pokemon getConfusedBy() {
        return confusedBy;
    }

    public void setConfusedBy(Pokemon confusedBy) {
        this.confusedBy = confusedBy;
    }

    public Pokemon getCursedBy() {
        return cursedBy;
    }

    public void setCursedBy(Pokemon cursedBy) {
        this.cursedBy = cursedBy;
    }

    public Pokemon getBindedBy() {
        return bindedBy;
    }

    public void setBindedBy(Pokemon bindedBy) {
        this.bindedBy = bindedBy;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(int line) {
        if (game.get(line + 1).contains("|replace|") && game.get(line + 1).contains("|Zor")) {
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> p.dead = true);
        } else {
            this.dead = true;
        }
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp, int turn) {
        if (zoroTurns.contains(turn))
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> p.hp = hp);
        else
            this.hp = hp;
    }

    public String getPokemon() {
        return pokemon;
    }

    public void setPokemon(String pokemon) {
        this.pokemon = pokemon;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public Pokemon getStatusedBy() {
        return statusedBy;
    }

    public void setStatusedBy(Pokemon statusedBy) {
        this.statusedBy = statusedBy;
    }

    public Pokemon getLastDmgBy() {
        return lastDmgBy;
    }

    public void setLastDmgBy(Pokemon lastDmgBy) {
        this.lastDmgBy = lastDmgBy;
    }

    public void killsPlus1(int turn) {
        if (this.zoroTurns.contains(turn)) {
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> {
                if (p.lastKillTurn == turn) return;
                p.kills++;
                p.lastKillTurn = turn;
            });
        } else {
            if (lastKillTurn == turn) return;
            this.kills++;
            this.lastKillTurn = turn;
        }
    }


    public Pokemon getPerishedBy() {
        return perishedBy;
    }


    public void setPerishedBy(Pokemon perishedBy) {
        this.perishedBy = perishedBy;
    }

    @Override
    public String toString() {
        return "Pokemon{" + "player=" + player +
                ", pokemon='" + pokemon + '\'' +
                ", kills=" + kills +
                ", dead=" + dead +
                '}';
    }
}
