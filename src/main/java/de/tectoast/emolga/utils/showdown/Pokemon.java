package de.tectoast.emolga.utils.showdown;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.function.Supplier;

public class Pokemon {
    private static final Logger logger = LoggerFactory.getLogger(Pokemon.class);
    private final Player player;
    private final List<Integer> zoroTurns;
    private final List<String> game;
    private final Supplier<String> disabledAbi;
    private final Map<Integer, String> zoru;
    private final Set<String> moves = new HashSet<>();
    private final String gender;
    private String pokemon;
    private int kills;
    private Pokemon statusedBy, bindedBy, cursedBy, seededBy, nightmaredBy, confusedBy, lastDmgBy, perishedBy;
    private boolean dead = false;
    private int hp = 100;
    private String ability = "";
    private int lastKillTurn = -1;
    private String nickname;
    private String item;


    public Pokemon(String poke, Player player, List<Integer> zoroTurns, List<String> game, Supplier<String> disabledAbi, Map<Integer, String> zoru, String gender) {
        pokemon = poke;
        this.zoroTurns = zoroTurns;
        this.game = game;
        this.player = player;
        this.disabledAbi = disabledAbi;
        this.zoru = zoru;
        this.gender = gender;
    }

    public Optional<String> getItem() {
        return Optional.ofNullable(item);
    }

    public void setItem(String item) {
        if (this.item == null) this.item = item;
    }


    public String getNickname() {
        return nickname == null ? pokemon : nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void addMove(String move) {
        moves.add(move);
    }

    public String buildGenderStr() {
        return gender != null ? " (%s)".formatted(gender.trim()) : "";
    }

    public Set<String> getMoves() {
        return moves;
    }

    public Player getPlayer() {
        return player;
    }

    public Optional<String> getAbility() {
        return Optional.ofNullable(ability.isEmpty() ? null : ability);
    }

    public void setAbility(String ability) {
        logger.debug("setting ability {} to {}...", ability, pokemon);
        if (this.ability.isEmpty()) {
            this.ability = ability;
        }
    }

    public boolean noAbilityTrigger(int line) {
        return !this.ability.isEmpty() && !Objects.equals(this.disabledAbi.get(), this.ability) && !game.get(line + 1).contains("[from] ability: " + this.ability);
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
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> {
                p.dead = true;
                zoru.remove(player.getNumber());
            });
        } else {
            this.dead = true;
        }
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp, int turn) {
        if (zoroTurns.contains(turn)) {
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> p.hp = hp);
            logger.info(MarkerFactory.getMarker("important"), "set hp zoroark in turn {} to {}", turn, hp);
        } else this.hp = hp;
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
            //if (lastKillTurn == turn) return;
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
        return "Pokemon{" + "player=" + player + ", pokemon='" + pokemon + '\'' + ", kills=" + kills + ", dead=" + dead + '}';
    }
}
