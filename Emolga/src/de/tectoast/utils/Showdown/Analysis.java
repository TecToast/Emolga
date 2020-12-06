package de.tectoast.utils.Showdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Analysis {

    public static Player[] analyse(String link) {

        if (link.indexOf("http:") == 0) {
            link = "https:" + link.split("http:")[1];
        }

        ArrayList<String> game = (ArrayList<String>) getGameArrayList(link).clone();

        Player p1 = new Player();
        Player p2 = new Player();

        for (String s : game) {
            //Nickname des Spielers
            if ((s.contains("|player|p1")) && (s.length() > 11)) {
                p1.setNickname(s.split("\\|")[3]);
            }
            if ((s.contains("|player|p2")) && (s.length() > 11)) {
                p2.setNickname(s.split("\\|")[3]);
            }

            //Welche commands sind dabei?
            if (s.contains("|poke|p1")) {
                String str = s.split("\\|")[3].split(",")[0];
                if (str.contains("Zoroark") || str.contains("Zorua")) return null;
                if (str.contains("Urshifu")) str = "Urshifu-Rapid-Strike";
                p1.getMons().add(new SDPokemon(str));
            }
            if (s.contains("|poke|p2")) {
                String str = s.split("\\|")[3].split(",")[0];
                if (str.contains("Zoroark") || str.contains("Zorua")) return null;
                if (str.contains("Urshifu")) str = "Urshifu-Rapid-Strike";
                p2.getMons().add(new SDPokemon(str));
            }

            //Nicks & Detailschange
            if (s.contains("|switch|p1") || s.contains("|drag|p1")) {
                if (s.split("\\|")[3].split(",")[0].contains("Silvally") && p1.indexOfName("Silvally-*") != -1) {//Silvally-Problem
                    p1.getMons().get(p1.indexOfName("Silvally-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Arceus") && p1.indexOfName("Arceus-*") != -1) {//Arceus-Problem
                    p1.getMons().get(p1.indexOfName("Arceus-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Genesect") && p1.indexOfName("Genesect-*") != -1) {//Genesect-Problem
                    p1.getMons().get(p1.indexOfName("Genesect-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Gourgeist") && p1.indexOfName("Gourgeist-*") != -1) {//Genesect-Problem
                    p1.getMons().get(p1.indexOfName("Gourgeist-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }//hier
                try {
                    p1.getMons().get(p1.indexOfName(s.split("\\|")[3].split(",")[0])).setNickname(s.split("\\|")[2].substring(5));
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }
            if (s.contains("|switch|p2") || s.contains("|drag|p2")) {
                if (s.split("\\|")[3].split(",")[0].contains("Silvally") && p2.indexOfName("Silvally-*") != -1) {//Silvally-Problem
                    p2.getMons().get(p2.indexOfName("Silvally-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Arceus") && p2.indexOfName("Arceus-*") != -1) {//Arceus-Problem
                    p2.getMons().get(p2.indexOfName("Arceus-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Genesect") && p2.indexOfName("Genesect-*") != -1) {//Genesect-Problem
                    p2.getMons().get(p2.indexOfName("Genesect-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Gourgeist") && p1.indexOfName("Gourgeist-*") != -1) {//Genesect-Problem
                    p1.getMons().get(p1.indexOfName("Gourgeist-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }
                if (s.split("\\|")[3].split(",")[0].contains("Urshifu") && p1.indexOfName("Urshifu-*") != -1) {//Genesect-Problem
                    p1.getMons().get(p1.indexOfName("Urshifu-*")).setPokemon(s.split("\\|")[3].split(",")[0]);
                }//Hier
                try {
                    String mon = s.split("\\|")[3].split(",")[0];
                    if (mon.contains("Gourgeist")) mon = "Gourgeist-*";
                    //System.out.println(p2.getMons().stream().map(commands::getPokemon).collect(Collectors.joining(",")));
                    p2.getMons().get(p2.indexOfName(mon)).setNickname(s.split("\\|")[2].substring(5));
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }

            //Win
            if (s.contains("|win|")) {
                if (p1.getNickname().equals(s.split("\\|")[2])) {
                    p1.setWinner(true);
                }
                if (p2.getNickname().equals(s.split("\\|")[2])) {
                    p2.setWinner(true);
                }
            }

            //Datailschange
            if (s.contains("|detailschange|p1")) {
                p1.getMons().get(p1.indexOfNick(s.split("\\|")[2].substring(5))).setPokemon(s.split("\\|")[3].split(",")[0]);
            }
            if (s.contains("|detailschange|p2")) {
                p2.getMons().get(p2.indexOfNick(s.split("\\|")[2].substring(5))).setPokemon(s.split("\\|")[3].split(",")[0]);
            }
        }


        SDPokemon lastMove = null;
        SDPokemon activeP1 = null;
        SDPokemon activeP2 = null;
        SDPokemon weatherBy = null;
        //Schaden
        for (String s : game) {
            /*
             * LastMove abspeichern
             */
            if (s.contains("|move|p1")) {
                lastMove = p1.getMons().get(p1.indexOfNick(s.split("\\|")[2].substring(5)));
            }
            if (s.contains("|move|p2")) {
                //System.out.println(s.split("\\|")[2].substring(5));
                lastMove = p2.getMons().get(p2.indexOfNick(s.split("\\|")[2].substring(5)));
            }

            /*
             * LastMove am Ende der Runde löschen
             */
            if (s.contains("|turn|")) {
                lastMove = null;
            }

            /*
             * aktive commands abspeichern und LastMove zuruecksetzen
             */
            if (s.contains("|switch|p1") || s.contains("|drag|p1")) {
                activeP1 = p1.getMons().get(p1.indexOfNick(s.split("\\|")[2].substring(5)));
                lastMove = null;
            }
            if (s.contains("|switch|p2") || s.contains("|drag|p2")) {
                //System.out.println(s);
                //System.out.println(s.split("\\|")[2]);
                /*for (commands mon : p2.getMons()) {
                    System.out.print(mon.getPokemon() + " ::: ");
                    m.out.println(mon.getNickname());
                }*/
                activeP2 = p2.getMons().get(p2.indexOfNick(s.split("\\|")[2].substring(5)));
                lastMove = null;
            }

            /*
             * Direkte Hits, ausgenommen von CurseSD
             */
            if ((s.contains("|-damage|p1")) && (s.split("\\|").length == 4)) {
                if (s.contains("0 fnt")) {
                    //Wenn CurseSD
                    if (lastMove == p1.getMons().get(p1.indexOfNick(s.split("\\|")[2].substring(5)))) {
                        if (lastMove.getLastDmgBy() != null) {
                            lastMove.getLastDmgBy().killsPlus1();
                            lastMove.setDead(true);
                        } else {
                            activeP1.setDead(true);
                            activeP2.killsPlus1();
                        }
                        //Wenn nicht CurseSD, also normaler Hit
                    } else {
                        activeP1.setDead(true);
                        activeP2.killsPlus1();
                    }
                } else {
                    activeP1.setLastDmgBy(activeP2);
                }
            }
            boolean b = (s.contains("|-damage|p2")) && (s.split("\\|").length == 4);
            if (b) {
                if (s.contains("0 fnt")) {
                    //Wenn CurseSD
                    if (lastMove == p2.getMons().get(p2.indexOfNick(s.split("\\|")[2].substring(5)))) {
                        if (lastMove.getLastDmgBy() != null) {
                            lastMove.getLastDmgBy().killsPlus1();
                            lastMove.setDead(true);
                        } else {
                            activeP2.setDead(true);
                            activeP1.killsPlus1();
                        }
                        //Wenn nicht CurseSD, also normaler Hit
                    } else {
                        activeP2.setDead(true);
                        activeP1.killsPlus1();
                    }
                } else {
                    activeP2.setLastDmgBy(activeP1);
                }
            }

            /*
             * Klammerattacken
             */
            //Eingesetzt
            if ((s.contains("|-activate|p1")) && (s.split("\\|").length == 5)) {

                activeP1.setBindedBy(activeP2);
            }
            if ((s.contains("|-activate|p2")) && (s.split("\\|").length == 5)) {
                activeP2.setBindedBy(activeP1);
            }
            //Kill
            if (s.contains("partiallytrapped")) {
                if ((s.contains("|-damage|p1")) && (s.split("\\|").length == 6)) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP1.getBindedBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP1.getBindedBy());
                    }
                }
                if ((s.contains("|-damage|p2")) && (s.split("\\|").length == 6)) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP2.getBindedBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP2.getBindedBy());
                    }
                }
            }

            /*
             * Destiny Bond
             */
            if ((s.contains("|-activate|p1")) && (s.contains("move: Destiny Bond"))) {
                activeP1.killsPlus1();
                activeP2.setDead(true);
            }
            if ((s.contains("|-activate|p2")) && (s.contains("move: Destiny Bond"))) {
                activeP2.killsPlus1();
                activeP1.setDead(true);
            }

            /*
             * Curse
             */
            //Eingesetzt
            if ((s.contains("|-start|p1")) && (s.contains("|Curse|"))) {
                activeP1.setCursedBy(activeP2);
            }
            if ((s.contains("|-start|p2")) && (s.contains("|Curse|"))) {
                activeP2.setCursedBy(activeP1);
            }
            //Kill
            if (s.contains("|[from] Curse")) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP1.getCursedBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP1.getCursedBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP2.getCursedBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP2.getCursedBy());
                    }
                }
            }

            /*
             * Perish Song
             */
            //eingesetzt
            if ((s.contains("|move|p1")) && (s.contains("|Perish Song|"))) {
                activeP1.setPerishedBy(activeP1);
                activeP2.setPerishedBy(activeP1);
            }
            if ((s.contains("|move|p2")) && (s.contains("|Perish Song|"))) {
                activeP1.setPerishedBy(activeP2);
                activeP2.setPerishedBy(activeP2);
            }
            //Kill
            if ((s.contains("|-start|p1")) && (s.contains("|perish0"))) {
                activeP1.setDead(true);
                if (activeP1 == activeP1.getPerishedBy()) activeP1.getLastDmgBy().killsPlus1();
                else activeP1.getPerishedBy().killsPlus1();
            }
            if ((s.contains("|-start|p2")) && (s.contains("|perish0"))) {
                activeP2.setDead(true);
                if (activeP2 == activeP2.getPerishedBy()) activeP2.getLastDmgBy().killsPlus1();
                else activeP2.getPerishedBy().killsPlus1();
            }

            /*
             * Seeds
             */
            //Eingesetzt
            if ((s.contains("|-start|p1")) && (s.contains("|move: Leech Seed"))) {
                activeP1.setSeededBy(activeP2);
            }
            if ((s.contains("|-start|p2")) && (s.contains("|move: Leech Seed"))) {
                activeP2.setSeededBy(activeP1);
            }
            //Kill
            if (s.contains("|[from] Leech Seed")) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP1.getSeededBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP1.getSeededBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP2.getSeededBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP2.getSeededBy());
                    }
                }
            }

            /*
             * Nightmare
             */
            //Eingesetzt
            if ((s.contains("|-start|p1")) && (s.contains("|Nightmare"))) {
                activeP1.setNightmaredBy(activeP2);
            }
            if ((s.contains("|-start|p2")) && (s.contains("|Nightmare"))) {
                activeP2.setNightmaredBy(activeP1);
            }
            //Kill
            if (s.contains("|[from] Nightmare")) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP1.getNightmaredBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP1.getNightmaredBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP2.getNightmaredBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP2.getNightmaredBy());
                    }
                }
            }

            /*
             * Confusion
             */
            //Eingesetzt
            if ((s.contains("|-start|p1")) && (s.contains("|confusion"))) {
                activeP1.setConfusedBy(activeP2);
            }
            if ((s.contains("|-start|p2")) && (s.contains("|confusion"))) {
                activeP2.setConfusedBy(activeP1);
            }
            //Kill
            if (s.contains("|[from] confusion")) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP1.getConfusedBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP1.getConfusedBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP2.getConfusedBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP2.getConfusedBy());
                    }
                }
            }

            /*
             * PSN BRN
             */
            //Eingesetzt
            if ((s.contains("|-status|p1"))) {
                if (s.contains("|[of] p")) {
                    activeP1.setStatusedBy(activeP2);
                } else if (lastMove != null) {
                    activeP1.setStatusedBy(lastMove);
                } else {
                    activeP1.setStatusedBy(p1.gettSpikesBy());
                }
            }
            if ((s.contains("|-status|p2"))) {
                if (s.contains("|[of] p")) {
                    activeP2.setStatusedBy(activeP1);
                } else if (lastMove != null) {
                    activeP2.setStatusedBy(lastMove);
                } else {
                    activeP2.setStatusedBy(p2.gettSpikesBy());
                }
            }
            //Kill
            if ((s.contains("|[from] psn")) || (s.contains("|[from] brn"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (activeP1.getStatusedBy() != null) {
                        if (s.contains("0 fnt")) {
                            activeP1.setDead(true);
                            activeP1.getStatusedBy().killsPlus1();
                        } else {
                            activeP1.setLastDmgBy(activeP1.getStatusedBy());
                        }
                    } else {
                        if (s.contains("0 fnt")) {
                            activeP1.setDead(true);
                            if (activeP1.getLastDmgBy() != null) {
                                activeP1.getLastDmgBy().killsPlus1();
                            } else {
                                activeP2.killsPlus1();
                            }
                        }
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (activeP2.getStatusedBy() != null) {
                        if (s.contains("0 fnt")) {
                            activeP2.setDead(true);
                            activeP2.getStatusedBy().killsPlus1();
                        } else {
                            activeP2.setLastDmgBy(activeP2.getStatusedBy());
                        }
                    } else {
                        if (s.contains("0 fnt")) {
                            activeP2.setDead(true);
                            if (activeP2.getLastDmgBy() != null) {
                                activeP2.getLastDmgBy().killsPlus1();
                            } else {
                                activeP1.killsPlus1();
                            }
                        }
                    }
                }
            }


            /*
             * HJK, JK, Life Orb, Recoil, Black Sludge, Sticky Barb, Solar Power, Dry Skin
             */
            if ((s.contains("|[from] High Jump Kick")) || (s.contains("|[from] Jump Kick")) || (s.contains("|[from] item: Life Orb")) || (s.contains("|[from] Recoil")) || (s.contains("|[from] recoil"))
                    || (s.contains("|[from] item: Black Sludge")) || (s.contains("|[from] item: Sticky Barb")) || (s.contains("|[from] ability: Solar Power")) || (s.contains("|[from] ability: Dry Skin"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        if (activeP1.getLastDmgBy() != null) {
                            activeP1.getLastDmgBy().killsPlus1();
                        } else {
                            activeP2.killsPlus1();
                        }
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        if (activeP2.getLastDmgBy() != null) {
                            activeP2.getLastDmgBy().killsPlus1();
                        } else {
                            activeP1.killsPlus1();
                        }
                    }
                }
            }

            /*
             * Liquid Ooze, Aftermath, Rough Skin, Iron Barbs, Bad Dreams, Rocky Helmet, Spiky Shield, Rowap Berry, Jaboca Berry
             */
            if ((s.contains("|[from] jumpkick")) || (s.contains("|[from] highjumpkick")) || (s.contains("|[from] ability: Liquid Ooze")) || (s.contains("|[from] ability: Aftermath")) || (s.contains("|[from] ability: Rough Skin")) || (s.contains("|[from] ability: Iron Barbs"))
                    || (s.contains("|[from] ability: Bad Dreams")) || (s.contains("|[from] item: Rocky Helmet")) || (s.contains("|[from] Spiky Shield")) || (s.contains("|[from] item: Rowap Berry"))
                    || (s.contains("|[from] item: Jaboca Berry"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        activeP2.killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(activeP2);
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        activeP1.killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(activeP1);
                    }
                }
            }

            /*
             * Powder
             */
            if ((s.contains("|-damage|p1")) && (s.contains("[silent]"))) {
                if (s.contains("0 fnt")) {
                    activeP1.setDead(true);
                    activeP2.killsPlus1();
                } else {
                    activeP1.setLastDmgBy(activeP2);
                }
            }
            if ((s.contains("|-damage|p2")) && (s.contains("[silent]"))) {
                if (s.contains("0 fnt")) {
                    activeP2.setDead(true);
                    activeP1.killsPlus1();
                } else {
                    activeP2.setLastDmgBy(activeP1);
                }
            }

            /*
             * Weather
             */
            //Eingesetzt
            if ((s.contains("|-weather|"))) {
                if ((s.contains("|[of] p1"))) {
                    weatherBy = activeP1;
                } else if ((s.contains("|[of] p2"))) {
                    weatherBy = activeP2;
                } else if (!s.contains("|[upkeep]") && !s.contains("|none")) {
                    weatherBy = lastMove;
                }
            }
            //Kill
            if ((s.contains("|[from] Sandstorm")) || (s.contains("|[from] Hail"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        weatherBy.killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(weatherBy);
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        weatherBy.killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(weatherBy);
                    }
                }
            }

            /*
             * Rocks
             */
            //Eingesetzt
            if ((s.contains("|-sidestart|p1")) && (s.contains("|move: Stealth Rock"))) {
                p1.setRocksBy(activeP2);
            }
            if ((s.contains("|-sidestart|p2")) && (s.contains("|move: Stealth Rock"))) {
                p2.setRocksBy(activeP1);
            }
            //Kill
            if ((s.contains("|[from] Stealth Rock"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        p1.getRocksBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(p1.getRocksBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        p2.getRocksBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(p2.getRocksBy());
                    }
                }
            }

            /*
             * Spikes
             */
            //Eingesetzt
            if ((s.contains("|-sidestart|p1")) && (s.contains("|Spikes"))) {
                p1.setSpikesBy(activeP2);
            }
            if ((s.contains("|-sidestart|p2")) && (s.contains("|Spikes"))) {
                p2.setSpikesBy(activeP1);
            }
            //Kill
            if ((s.contains("|[from] Spikes"))) {
                if ((s.contains("|-damage|p1"))) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(true);
                        p1.getSpikesBy().killsPlus1();
                    } else {
                        activeP1.setLastDmgBy(p1.getSpikesBy());
                    }
                }
                if ((s.contains("|-damage|p2"))) {
                    if (s.contains("0 fnt")) {
                        activeP2.setDead(true);
                        p2.getSpikesBy().killsPlus1();
                    } else {
                        activeP2.setLastDmgBy(p2.getSpikesBy());
                    }
                }
            }

            /*
             * TSpikes
             */
            //Eingesetzt
            if ((s.contains("|-sidestart|p1")) && (s.contains("|move: Toxic Spikes"))) {
                p1.settSpikesBy(activeP2);
            }
            if ((s.contains("|-sidestart|p2")) && (s.contains("|move: Toxic Spikes"))) {
                p2.settSpikesBy(activeP1);
            }

            /*
             * Lunar Dance, Healing Wish
             */
            if ((s.contains("|Lunar Dance|")) || (s.contains("|Healing Wish|"))) {
                if ((s.contains("|move|p1"))) {
                    if ((!s.contains("|[still]"))) {
                        activeP1.setDead(true);
                        if (activeP1.getLastDmgBy() != null) {
                            activeP1.getLastDmgBy().killsPlus1();
                        } else {
                            activeP2.killsPlus1();
                        }
                    }
                }
                if ((s.contains("|move|p2"))) {
                    if ((!s.contains("|[still]"))) {
                        activeP2.setDead(true);
                        if (activeP2.getLastDmgBy() != null) {
                            activeP2.getLastDmgBy().killsPlus1();
                        } else {
                            activeP1.killsPlus1();
                        }
                    }
                }
            }

            /*
             * Final Gambit, Memento
             */
            if ((s.contains("|Final Gambit|")) || (s.contains("|Memento|"))) {
                if ((s.contains("|move|p1"))) {
                    if ((!s.contains("|[notarget]"))) {
                        activeP1.setDead(true);
                        if (activeP1.getLastDmgBy() != null) {
                            activeP1.getLastDmgBy().killsPlus1();
                        } else {
                            activeP2.killsPlus1();
                        }
                    }
                }
                if ((s.contains("|move|p2"))) {
                    if ((!s.contains("|[notarget]"))) {
                        activeP2.setDead(true);
                        if (activeP2.getLastDmgBy() != null) {
                            activeP2.getLastDmgBy().killsPlus1();
                        } else {
                            activeP1.killsPlus1();
                        }
                    }
                }
            }

            /*
             * Explosion, Self Destruct
             */
            if ((s.contains("|Explosion|")) || (s.contains("|Self-Destruct|"))) {
                if ((s.contains("|move|p1"))) {
                    activeP1.setDead(true);
                    if (activeP1.getLastDmgBy() != null) {
                        activeP1.getLastDmgBy().killsPlus1();
                    } else {
                        activeP2.killsPlus1();
                    }
                }
                if ((s.contains("|move|p2"))) {
                    activeP2.setDead(true);
                    if (activeP2.getLastDmgBy() != null) {
                        activeP2.getLastDmgBy().killsPlus1();
                    } else {
                        activeP1.killsPlus1();
                    }
                }
            }
        }
        /*System.out.println(p1.getMons().get(0).getPokemon() + " " + p1.getMons().get(0).isDead() + " Kills:" + p1.getMons().get(0).getKills());
        System.out.println(p1.getMons().get(1).getPokemon() + " " + p1.getMons().get(1).isDead() + " Kills:" + p1.getMons().get(1).getKills());
        System.out.println(p1.getMons().get(2).getPokemon() + " " + p1.getMons().get(2).isDead() + " Kills:" + p1.getMons().get(2).getKills());
        System.out.println(p1.getMons().get(3).getPokemon() + " " + p1.getMons().get(3).isDead() + " Kills:" + p1.getMons().get(3).getKills());
        System.out.println(p1.getMons().get(4).getPokemon() + " " + p1.getMons().get(4).isDead() + " Kills:" + p1.getMons().get(4).getKills());
        System.out.println(p1.getMons().get(5).getPokemon() + " " + p1.getMons().get(5).isDead() + " Kills:" + p1.getMons().get(5).getKills());
        System.out.println(p2.getMons().get(0).getPokemon() + " " + p2.getMons().get(0).isDead() + " Kills:" + p2.getMons().get(0).getKills());
        System.out.println(p2.getMons().get(1).getPokemon() + " " + p2.getMons().get(1).isDead() + " Kills:" + p2.getMons().get(1).getKills());
        System.out.println(p2.getMons().get(2).getPokemon() + " " + p2.getMons().get(2).isDead() + " Kills:" + p2.getMons().get(2).getKills());
        System.out.println(p2.getMons().get(3).getPokemon() + " " + p2.getMons().get(3).isDead() + " Kills:" + p2.getMons().get(3).getKills());
        System.out.println(p2.getMons().get(4).getPokemon() + " " + p2.getMons().get(4).isDead() + " Kills:" + p2.getMons().get(4).getKills());
        System.out.println(p2.getMons().get(5).getPokemon() + " " + p2.getMons().get(5).isDead() + " Kills:" + p2.getMons().get(5).getKills());
        System.out.println("Player 1 wins(" + p1.getNickname() + "): " + p1.isWinner());
        System.out.println("Player 2 wins(" + p2.getNickname() + "): " + p2.isWinner());*/

        return new Player[]{p1, p2};


    }


    public static ArrayList<String> getGameArrayList(String link) {
        URL url;
        try {
            url = new URL(link);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));

            String inputLine;
            ArrayList<String> h = new ArrayList<>();
            //ArrayList<String> bull = new ArrayList<>();
            boolean isBull = true;

            while ((inputLine = in.readLine()) != null) {
                if (!isBull) //bull.add(inputLine);

                    h.add(inputLine);


                if (inputLine.contains("|j|")) {
                    isBull = false;
                }

                if (inputLine.contains("|win|")) {
                    isBull = true;
                }
            }
            in.close();

            return h;
        } catch (IOException e) {
            throw new ArithmeticException("Fehler");
        }


    }
}
