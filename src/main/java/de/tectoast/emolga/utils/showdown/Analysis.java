package de.tectoast.emolga.utils.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.pokemon.WeaknessCommand;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.tectoast.emolga.commands.Command.*;

public class Analysis {

    private static final Logger logger = LoggerFactory.getLogger(Analysis.class);

    private final String link;
    private final Map<Integer, Player> pl = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> zoroTurns = new HashMap<>();
    private final Map<Integer, String> zoru = new HashMap<>();
    private final Map<Integer, Pokemon> activeP = new HashMap<>();
    private final Map<Integer, Pokemon> futureSightBy = new HashMap<>();
    private final Map<Integer, Boolean> futureSight = new HashMap<>();
    private final Map<Integer, Pokemon> doomDesireBy = new HashMap<>();
    private final Map<Integer, Boolean> doomDesire = new HashMap<>();
    private final Map<Integer, List<String>> actMons = new HashMap<>();
    private final Map<Integer, String> actMon = new HashMap<>();
    private boolean randomBattle = false;
    private Pokemon lastMove;
    private Pokemon weatherBy;
    private String s;
    private String[] split;
    private int line = -1;
    private int turn;
    private String disabledAbility;
    private final Supplier<String> abiSupplier = () -> disabledAbility;

    private static final Collection<String> unknownFormes = List.of("Silvally", "Arceus", "Genesect", "Gourgeist", "Urshifu", "Zacian", "Zamazenta", "Xerneas");

    public Analysis(String link, Message m) {
        this.link = link;
        if (m != null) DBManagers.REPLAY_CHECK.set(m.getChannel().getIdLong(), m.getIdLong());
        for (int i = 1; i <= 2; i++) {
            pl.put(i, new Player(i));
            zoroTurns.put(i, new LinkedList<>());
            actMons.put(i, new LinkedList<>());
        }
    }

    private Pokemon getZoro(int i, String reason) {
        logger.warn("Requested Zoro from player {} in turn {} Reason: {}", i, turn, reason);
        return pl.get(i).getMons().get(pl.get(i).indexOfName(zoru.getOrDefault(i, "")));
    }

    private static void check(IntFunction<Boolean> ch, Consumer<Integer> c) {
        for (int i = 1; i <= 2; i++) {
            if (ch.apply(i)) c.accept(i);
        }
    }

    public Player[] analyse() throws IOException {
        logger.info("Reading URL... {}", link);
        List<String> game = new BufferedReader(new InputStreamReader(new URL(link + ".log").openConnection().getInputStream())).lines().toList();
        logger.info("Starting analyse!");
        long time = System.currentTimeMillis();
        for (String currentLine : game) {
            this.s = currentLine;
            this.split = s.split("\\|");
            checkPlayer(i -> s.contains("|player|p" + i) && s.length() > 11, p -> p.setNickname(split[3]));
            check(i -> s.contains("|poke|p" + i), i -> {
                String[] spl = split[3].split(",");
                String poke = spl[0];
                pl.get(i).getMons().add(new Pokemon(poke, pl.get(i), zoroTurns.get(i), game, abiSupplier, zoru, spl.length == 1 ? null : spl[1]));
                if (poke.equals("Zoroark") || poke.equals("Zorua")) zoru.put(i, poke);
            });
            checkPlayer(i -> s.contains("|teamsize|p" + i), p -> p.setTeamsize(Integer.parseInt(split[3])));
            check(i -> s.contains("|switch|p" + i) || s.contains("|drag|p" + i), i -> {
                String[] spl = split[3].split(",");
                String pokemon = spl[0];
                Player p = pl.get(i);
                if (p.getMons().size() == 0 && !randomBattle) randomBattle = true;
                if (randomBattle) {
                    if (p.indexOfName(pokemon) == -1) {
                        logger.info("Adding {} to {}...", pokemon, p.getNickname());
                        p.getMons().add(new Pokemon(pokemon, p, zoroTurns.get(i), game, abiSupplier, zoru, Arrays.stream(spl).map(String::trim).filter(str -> {
                            if (str.equals("F")) return true;
                            return str.equals("M");
                        }).findFirst().orElse(null)));
                    }
                } else {
                    //unknownFormes.stream().filter(pokemon::contains).filter(str -> p.indexOfName(str + "-*") != 1).forEach(str -> p.getMons().get(p.indexOfName(str + "-*")).setPokemon(pokemon));
                    if (pokemon.contains("Silvally") && p.indexOfName("Silvally-*") != -1) {//Silvally-Problem
                        p.getMons().get(p.indexOfName("Silvally-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Arceus") && p.indexOfName("Arceus-*") != -1) {//Arceus-Problem
                        p.getMons().get(p.indexOfName("Arceus-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Genesect") && p.indexOfName("Genesect-*") != -1) {//Genesect-Problem
                        p.getMons().get(p.indexOfName("Genesect-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Gourgeist") && p.indexOfName("Gourgeist-*") != -1) {//Gourgeist-Problem
                        p.getMons().get(p.indexOfName("Gourgeist-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Urshifu") && p.indexOfName("Urshifu-*") != -1) {//Urshifu-Problem
                        p.getMons().get(p.indexOfName("Urshifu-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Zacian") && p.indexOfName("Zacian-*") != -1) {//Zacian-Problem
                        p.getMons().get(p.indexOfName("Zacian-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Zamazenta") && p.indexOfName("Zamazenta-*") != -1) {//Zacian-Problem
                        p.getMons().get(p.indexOfName("Zamazenta-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Xerneas") && p.indexOfName("Xerneas-*") != -1) {//Xerneas-Problem
                        p.getMons().get(p.indexOfName("Xerneas-*")).setPokemon(pokemon);
                    }
                }
            });
            check(i -> s.contains("|switch|p" + i) || s.contains("|drag|p" + i) || s.contains("|replace|p" + i), i -> actMon.put(i, split[3].split(",")[0]));
            for (int i = 1; i <= 2; i++) {
                actMons.computeIfAbsent(i, x -> new LinkedList<>()).add(actMon.getOrDefault(i, ""));
            }
            checkPlayer(i -> s.contains("|win|" + pl.get(i).getNickname()), p -> p.setWinner(true));
        }

        Collections.reverse(actMons.get(1));
        Collections.reverse(actMons.get(2));

        List<String> reversedGame = new ArrayList<>(game);
        Collections.reverse(reversedGame);

        check(zoru::containsKey, i -> {
            int t;
            boolean isZ = false;
            int x = 0;
            JSONObject learnset = getLearnsetJSON();
            JSONObject dex = getDataJSON();
            for (String s : reversedGame) {
                if (s.contains("|turn|")) {
                    t = Integer.parseInt(s.substring(6));
                    if (isZ) zoroTurns.get(i).add(t);
                }
                if (s.contains("|replace|p" + i) && s.contains("|Zor")) {
                    isZ = true;
                    logger.info(MarkerFactory.getMarker("important"), "isZ REPLACE");
                }
                if (s.contains("|move|p" + i)) {
                    JSONObject o = dex.getJSONObject(toSDName(actMons.get(i).get(x)));
                    if (!learnset.getJSONObject(toSDName(o.optString("baseSpecies", o.getString("name")))).getJSONObject("learnset").keySet().contains(toSDName(s.split("\\|")[3]))) {
                        isZ = true;
                        logger.info(MarkerFactory.getMarker("important"), "isZ MOVE");
                    }
                }
                if (s.contains("|switch|p" + i) || s.contains("|drag|p" + i)) isZ = false;
                x++;
            }
        });
        line = -1;
        logger.info(MarkerFactory.getMarker("important"), "zoroTurns.get(1) = {}", zoroTurns.get(1));
        for (String currentLine : game) {
            line++;
            s = currentLine;
            split = s.split("\\|");
            check(i -> s.contains("|switch|p" + i) || s.contains("|drag|p" + i) || s.contains("|replace|p" + i), i -> {
                Pokemon mon = pl.get(i).getMons().get(pl.get(i).indexOfName(split[3].split(",")[0]));
                mon.setNickname(split[2].split(":")[1].trim());
                lastMove = null;
                if (!s.contains("|replace|") && zoru.containsKey(i)) {
                    boolean noabi = mon.noAbilityTrigger(line);
                    if (noabi || mon.checkHPZoro(Integer.parseInt(split[4].split("/")[0]))) {
                        activeP.put(i, getZoro(i, noabi ? "NoAbilityTrigger" : "HPZoro"));
                    } else {
                        activeP.put(i, mon);
                    }
                } else {
                    activeP.put(i, mon);
                }
            });
            checkPokemon(i -> s.contains("|move|p" + i), p -> {
                lastMove = p;
                p.addMove(split[3]);
            });
            checkPokemon(i -> s.contains("|-activate|p" + i) && (s.contains("|ability: Synchronize") || s.contains("|move: Protect")), p -> lastMove = p);
            if (s.contains("|turn|")) {
                lastMove = null;
                for (int i = 1; i <= 2; i++) {
                    futureSight.put(i, false);
                    doomDesire.put(i, false);
                }
                turn = Integer.parseInt(s.substring(6));
            }
            checkPokemon(i -> s.contains("|detailschange|p" + i), p -> p.setPokemon(split[3].split(",")[0]));
            check(i -> s.contains("|-activate|p" + i) && s.contains("move: Court Change"), i -> {
                Player p1 = pl.get(i);
                Player p2 = pl.get(3 - i);
                Pokemon activeP1 = activeP.get(i);
                Pokemon activeP2 = activeP.get(3 - i);
                if (p1.getSpikesBy(activeP2) != null) p2.setSpikesBy(activeP1);
                if (p1.getRocksBy(activeP2) != null) p2.setRocksBy(activeP1);
                if (p1.getSpikesBy(activeP2) != null) p2.settSpikesBy(activeP1);
                if (p2.getSpikesBy(activeP1) != null) p1.setSpikesBy(activeP1);
                else p1.setSpikesBy(null);
                if (p2.getRocksBy(activeP1) != null) p1.setRocksBy(activeP1);
                else p1.setRocksBy(null);
                if (p2.gettSpikesBy(activeP1) != null) p1.settSpikesBy(activeP1);
                else p1.settSpikesBy(null);
            });
            check(zoru::containsKey, i -> {
                Pokemon activeP1 = activeP.get(i);
                if (s.contains("|-damage|p" + i)) {
                    Player p1 = pl.get(i);
                    int oldHP = activeP1.getHp();
                    String lifes = split[3].split("/")[0];
                    int newHp;
                    if (lifes.contains("fnt")) newHp = 0;
                    else newHp = Integer.parseInt(lifes);
                    if (s.contains("[from] Stealth Rock")) {
                        int dif = oldHP - newHp;
                        if (WeaknessCommand.getEffectiveness("Rock", Command.getDataJSON().getJSONObject(toSDName(activeP1.getPokemon())).getStringList("types").toArray(String[]::new)) != 0 && dif > 10 && dif < 14)
                            activeP1 = getZoro(i, "Stealth Rock");
                    } else if (s.contains("[from] Spikes")) {
                        JSONObject mon = Command.getDataJSON().getJSONObject(toSDName(activeP1.getPokemon()));
                        if (mon.getStringList("types").contains("Flying") || mon.getJSONObject("abilities").toMap().containsValue("Levitate"))
                            activeP1 = getZoro(i, "Spikes");
                    }
                    activeP1.setHp(newHp, turn);
                    activeP.put(i, activeP1);
                } else if (s.contains("|-heal|p" + i)) {
                    activeP1.setHp(Integer.parseInt(split[3].split("/")[0]), turn);
                } else if (s.contains("|-activate|p" + i) && s.contains("|move: Sticky Web")) {
                    Player p1 = pl.get(i);
                    JSONObject mon = Command.getDataJSON().getJSONObject(toSDName(activeP1.getPokemon()));
                    if (mon.getStringList("types").contains("Flying") || mon.getJSONObject("abilities").toMap().containsValue("Levitate"))
                        activeP.put(i, getZoro(i, "Sticky Web"));
                } else if (s.contains("[from] ability:") && s.contains("[of] p" + i)) {
                    Arrays.stream(split).filter(str -> str.contains("[from] ability:")).map(str -> str.split(":")[1].trim()).forEach(str -> activeP.get(i).setAbility(str));
                } else if (s.contains("|-ability|p" + i)) {
                    activeP.get(i).setAbility(split[3].trim());
                }
            });
            checkPokemon(i -> s.contains("[from] ability:") && s.contains("[of] p" + i), p -> Arrays.stream(split).filter(str -> str.contains("[from] ability:")).map(str -> str.split(":")[1].trim()).forEach(p::setAbility));
            checkPokemon(i -> s.contains("|-ability|p" + i), p -> p.setAbility(split[3].trim()));
            check(i -> s.contains("|-damage|p" + i) && split.length == 4, i -> {
                Pokemon activeP1 = activeP.get(i);
                Pokemon activeP2 = activeP.get(3 - i);
                if (futureSight.get(3 - i)) {
                    if (s.contains("0 fnt")) {
                        activeP.get(i).setDead(line);
                        futureSightBy.get(3 - i).killsPlus1(turn);
                    } else {
                        activeP1.setLastDmgBy(futureSightBy.get(3 - i));
                    }
                } else if (doomDesire.get(3 - i)) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line);
                        doomDesireBy.get(3 - i).killsPlus1(turn);
                    } else {
                        activeP1.setLastDmgBy(doomDesireBy.get(3 - i));
                    }
                } else {
                    if (s.contains("0 fnt")) {
                        //Wenn CurseSD
                        if (lastMove == activeP1) {
                            if (lastMove.getLastDmgBy() != null) {
                                lastMove.getLastDmgBy().killsPlus1(turn);
                                lastMove.setDead(line);
                            } else {
                                activeP1.setDead(line);
                                activeP2.killsPlus1(turn);
                            }
                            //Wenn nicht CurseSD, also normaler Hit
                        } else {
                            activeP1.setDead(line);
                            activeP2.killsPlus1(turn);
                        }
                    } else {
                        activeP1.setLastDmgBy(activeP2);
                    }
                }
            });
            checkPokemonBoth(i -> s.contains("|-activate|p" + i) && split.length == 5, Pokemon::setBindedBy);
            checkPokemon(i -> s.contains("partiallytrapped") && s.contains("|-damage|p" + i) && split.length == 6, p -> {
                if (s.contains("0 fnt")) {
                    p.setDead(line);
                    p.getBindedBy().killsPlus1(turn);
                } else {
                    p.setLastDmgBy(p.getBindedBy());
                }
            });
            checkPokemonBoth(i -> s.contains("|-activate|p" + i) && s.contains("move: Destiny Bond"), (p1, p2) -> {
                p1.killsPlus1(turn);
                p2.setDead(line);
            });
            checkPokemonBoth(i -> s.contains("|-start|p" + i) && s.contains("|Curse|"), Pokemon::setCursedBy);
            checkPokemon(i -> s.contains("|[from] Curse") && s.contains("|-damage|p" + i), activeP1 -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP1.getCursedBy().killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP1.getCursedBy());
                }
            });
            checkPokemonBoth(i -> s.contains("|move|p" + i) && s.contains("|Perish Song|"), (activeP1, activeP2) -> {
                activeP1.setPerishedBy(activeP1);
                activeP2.setPerishedBy(activeP1);
            });
            checkPokemon(i -> s.contains("|-start|p" + i) && s.contains("|perish0"), activeP1 -> {
                activeP1.setDead(line);
                if (activeP1 == activeP1.getPerishedBy()) activeP1.getLastDmgBy().killsPlus1(turn);
                else activeP1.getPerishedBy().killsPlus1(turn);
            });
            checkPokemonBoth(i -> s.contains("|-start|p" + i) && s.contains("|move: Leech Seed"), Pokemon::setSeededBy);
            checkPokemon(i -> s.contains("|[from] Leech Seed") && s.contains("|-damage|p" + i), activeP1 -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP1.getSeededBy().killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP1.getSeededBy());
                }
            });
            checkPokemonBoth(i -> s.contains("|-start|p" + i) && s.contains("|Nightmare"), Pokemon::setNightmaredBy);
            checkPokemon(i -> s.contains("|[from] Nightmare") && s.contains("|-damage|p" + i), activeP1 -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP1.getNightmaredBy().killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP1.getNightmaredBy());
                }
            });
            checkPokemonBoth(i -> s.contains("|-start|p" + i) && s.contains("|confusion"), Pokemon::setConfusedBy);
            checkPokemon(i -> s.contains("|[from] confusion") && s.contains("|-damage|p" + i), activeP1 -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP1.getConfusedBy().killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP1.getConfusedBy());
                }
            });
            check(i -> s.contains("|-start|p" + i) && s.contains("Future Sight"), i -> futureSightBy.put(i, activeP.get(i)));
            check(i -> s.contains("|-start|p" + i) && s.contains("Doom Desire"), i -> doomDesireBy.put(i, activeP.get(i)));
            check(i -> s.contains("|-end|p" + i) && s.contains("Future Sight"), i -> {
                lastMove = futureSightBy.get(3 - i);
                futureSight.put(3 - i, true);
            });
            check(i -> s.contains("|-end|p" + i) && s.contains("Doom Desire"), i -> {
                lastMove = doomDesireBy.get(3 - i);
                doomDesire.put(3 - i, true);
            });
            check(i -> s.contains("|-status|p" + i), i -> {
                Pokemon activeP1 = activeP.get(i);
                Pokemon activeP2 = activeP.get(3 - i);
                if (s.contains("|[of] p")) {
                    activeP1.setStatusedBy(activeP2);
                } else if (s.contains("|[from] item:")) {
                    activeP1.setStatusedBy(null);
                } else if (lastMove != null) {
                    activeP1.setStatusedBy(lastMove);
                } else {
                    activeP1.setStatusedBy(pl.get(i).gettSpikesBy(activeP2));
                }
            });
            checkPokemonBoth(i -> (s.contains("|[from] psn") || s.contains("|[from] brn")) && s.contains("|-damage|p" + i), (activeP1, activeP2) -> {
                if (activeP1.getStatusedBy() != null) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line);
                        activeP1.getStatusedBy().killsPlus1(turn);
                    } else {
                        activeP1.setLastDmgBy(activeP1.getStatusedBy());
                    }
                } else {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line);
                        if (activeP1.getLastDmgBy() != null) {
                            activeP1.getLastDmgBy().killsPlus1(turn);
                        } else {
                            activeP2.killsPlus1(turn);
                        }
                    }
                }
            });

            check(i -> s.contains("[from] item:") && s.contains("|p" + i), i -> (s.contains("[of] p" + (3 - i)) ? activeP.get(3 - i) : activeP.get(i)).setItem(Arrays.stream(split).filter(str -> str.contains("[from] item")).map(str -> str.split(":")[1].trim()).findFirst().orElse(null)));

            checkPokemonBoth(i -> Stream.of("|[from] High Jump Kick", "|[from] Jump Kick", "|[from] item: Life Orb", "|[from] Recoil", "|[from] recoil", "|[from] item: Black Sludge", "|[from] item: Sticky Barb", "|[from] ability: Solar Power", "|[from] ability: Dry Skin", "|[from] mindblown").anyMatch(s::contains) && s.contains("|-damage|p" + i), (activeP1, activeP2) -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    if (activeP1.getLastDmgBy() != null) {
                        activeP1.getLastDmgBy().killsPlus1(turn);
                    } else {
                        activeP2.killsPlus1(turn);
                    }
                }
            });
            checkPokemonBoth(i -> Stream.of("|[from] jumpkick", "|[from] highjumpkick", "|[from] ability: Liquid Ooze", "|[from] ability: Aftermath", "|[from] ability: Rough Skin", "|[from] ability: Iron Barbs", "|[from] ability: Bad Dreams", "|[from] item: Rocky Helmet", "|[from] Spiky Shield", "|[from] item: Rowap Berry", "|[from] item: Jaboca Berry").anyMatch(s::contains) && s.contains("|-damage|p" + i), (activeP1, activeP2) -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP2.killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP2);
                }
            });
            checkPokemonBoth(i -> s.contains("|-damage|p" + i) && s.contains("[silent]"), (activeP1, activeP2) -> {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line);
                    activeP2.killsPlus1(turn);
                } else {
                    activeP1.setLastDmgBy(activeP2);
                }
            });
            checkPokemon(i -> s.contains("|-weather|") && s.contains("|[of] p" + i), p -> weatherBy = p);
            if (s.contains("|-weather|") && !s.contains("|[upkeep]") && !s.contains("|none") && !s.contains("|[of] p"))
                weatherBy = lastMove;
            if (s.contains("|-weather|")) {
                disabledAbility = switch (split[2]) {
                    case "SunnyDay" -> "Drought";
                    case "RainDance" -> "Drizzle";
                    case "Sandstorm" -> "Sand Stream";
                    case "Hail" -> "Snow Warning";
                    case "none" -> "None";
                    default -> throw new IllegalStateException("Unexpected value: " + split[2]);
                };
            }
            check(i -> (s.contains("|[from] Sandstorm") || s.contains("|[from] Hail")) && s.contains("|-damage|p" + i), i -> {
                Pokemon activeP1 = activeP.get(i);
                if (pl.get(3 - i).getMons().contains(weatherBy)) { //Abfrage, ob das Weather von einem gegnerischem Mon kommt
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line);
                        weatherBy.killsPlus1(turn);
                    } else {
                        activeP1.setLastDmgBy(weatherBy);
                    }
                } else {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line);
                        activeP1.getLastDmgBy().killsPlus1(turn);
                    }
                }
            });
            check(i -> s.contains("|-sidestart|p" + i) && s.contains("|move: Stealth Rock"), i -> pl.get(i).setRocksBy(activeP.get(3 - i)));
            check(i -> s.contains("|[from] Stealth Rock") && s.contains("|-damage|p" + i), i -> {
                if (s.contains("0 fnt")) {
                    activeP.get(i).setDead(line);
                    pl.get(i).getRocksBy(activeP.get(3 - i)).killsPlus1(turn);
                } else {
                    activeP.get(i).setLastDmgBy(pl.get(i).getRocksBy(activeP.get(3 - i)));
                }
            });
            check(i -> s.contains("|-sidestart|p" + i) && s.contains("|Spikes"), i -> pl.get(i).setSpikesBy(activeP.get(3 - i)));
            check(i -> s.contains("|[from] Spikes") && s.contains("|-damage|p" + i), i -> {
                if (s.contains("0 fnt")) {
                    activeP.get(i).setDead(line);
                    pl.get(i).getSpikesBy(activeP.get(3 - i)).killsPlus1(turn);
                } else {
                    activeP.get(i).setLastDmgBy(pl.get(i).getSpikesBy(activeP.get(3 - i)));
                }
            });
            check(i -> s.contains("|-sidestart|p" + i) && s.contains("|move: Toxic Spikes"), i -> pl.get(i).settSpikesBy(activeP.get(3 - i)));
            checkPokemonBoth(i -> (s.contains("|Lunar Dance|") || s.contains("|Healing Wish|")) && s.contains("|move|p" + i), (activeP1, activeP2) -> {
                if (!s.contains("|[still]")) {
                    activeP1.setDead(line);
                    if (activeP1.getLastDmgBy() != null) {
                        activeP1.getLastDmgBy().killsPlus1(turn);
                    } else {
                        activeP2.killsPlus1(turn);
                    }
                }
            });
            checkPokemonBoth(i -> (s.contains("|Final Gambit|") || s.contains("|Memento|")) && s.contains("|move|p" + i), (activeP1, activeP2) -> {
                if (!s.contains("|[notarget]") && !s.contains("|[still]")) {
                    activeP1.setDead(line);
                    if (activeP1.getLastDmgBy() != null) {
                        activeP1.getLastDmgBy().killsPlus1(turn);
                    } else {
                        activeP2.killsPlus1(turn);
                    }
                }
            });
            checkPokemonBoth(i -> (s.contains("|Explosion|") || s.contains("|Self-Destruct|") || s.contains("|Misty Explosion|")) && s.contains("|move|p" + i), (activeP1, activeP2) -> {
                activeP1.setDead(line);
                if (activeP1.getLastDmgBy() != null) {
                    activeP1.getLastDmgBy().killsPlus1(turn);
                } else {
                    activeP2.killsPlus1(turn);
                }
            });
        }
        logger.info("TIME: " + (System.currentTimeMillis() - time) + " ==========================================================");
        return pl.values().toArray(Player[]::new);
    }

    private void checkPokemon(IntFunction<Boolean> ch, Consumer<Pokemon> active) {
        check(ch, i -> active.accept(activeP.get(i)));
    }

    private void checkPokemonBoth(IntFunction<Boolean> ch, BiConsumer<Pokemon, Pokemon> active) {
        check(ch, i -> active.accept(activeP.get(i), activeP.get(3 - i)));
    }

    private void checkPokemonOther(IntFunction<Boolean> ch, Consumer<Pokemon> active) {
        check(ch, i -> active.accept(activeP.get(3 - i)));
    }

    private void checkPlayer(IntFunction<Boolean> ch, Consumer<Player> player) {
        check(ch, i -> player.accept(pl.get(i)));
    }

    private void checkPlayerBoth(IntFunction<Boolean> ch, BiConsumer<Player, Player> player) {
        check(ch, i -> player.accept(pl.get(i), pl.get(3 - i)));
    }
}
