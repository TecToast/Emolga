package de.tectoast.emolga.utils.automation.collection;

import de.tectoast.emolga.utils.automation.structure.DocEntry;
import de.tectoast.emolga.utils.records.SorterData;
import de.tectoast.emolga.utils.records.StatLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static de.tectoast.emolga.commands.Command.*;

public class DocEntries {
    public static final DocEntry UPL = DocEntry.create()
            .leagueFunction((x, y) -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("UPL"))
            .killProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 7, plindex * 12 + 2 + monindex))
            .deathProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 23, plindex * 12 + 2 + monindex))
            .useProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 15, plindex * 12 + 2 + monindex))
            .zeroMapper()
            .winProcessor((plindex, gameday) -> new StatLocation("Stats", "AF", plindex * 12 + 1 + gameday))
            .looseProcessor((plindex, gameday) -> new StatLocation("Stats", "AG", plindex * 12 + 1 + gameday))
            .resultCreator((DocEntry.BasicResultCreator) (b, gdi, index, numberOne, numberTwo, url) -> b.addRow("Spielplan!%s%d".formatted(getAsXCoord(gdi / 4 * 6 + 4), gdi % 4 * 6 + 6 + index), Arrays.asList(
                    numberOne, "=HYPERLINK(\"%s\"; \":\")".formatted(url), numberTwo
            )))
            .setStatIfEmpty(false)
            .sorterData(new SorterData("Tabelle!B2:H9", "Tabelle!B2:H9", false, null, 1, 4, 2));

    public static final DocEntry PRISMA = DocEntry.create()
            .leagueFunction((x, y) -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("Prisma"))
            .killProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 6, plindex * 11 + 2 + monindex))
            .deathProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 14, plindex * 11 + 2 + monindex))
            .zeroMapper()
            .winProcessor((plindex, gameday) -> new StatLocation("Data", "W", plindex * 11 + 1 + gameday))
            .looseProcessor((plindex, gameday) -> new StatLocation("Data", "X", plindex * 11 + 1 + gameday))
            .resultCreator((DocEntry.BasicResultCreator) (b, gdi, index, numberOne, numberTwo, url) -> b.addSingle("Spielplan!%s%d".formatted(
                    getAsXCoord((gdi == 6 ? 1 : gdi % 3) * 3 + 3),
                    gdi / 3 * 5 + 4 + index
            ), "=HYPERLINK(\"%s\"; \"%d:%d\")".formatted(url, numberOne, numberTwo)))
            .setStatIfEmpty(false)
            .sorterData(new SorterData("Tabelle!B2:I9", "Tabelle!B2:I9",
                    true, s -> Integer.parseInt(s.substring("=Data!W".length())) / 11 - 1, 1, -1, 7));

    public static final DocEntry NDS = DocEntry.create()
            .leagueFunction((x, y) -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS"))
            .tableMapper(nds -> nds.getStringList("table").stream().map(s -> reverseGet(nds.getJSONObject("teamnames"), s))
                    .map(Long::parseLong).toList())
            .killProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 6, plindex * 17 + 2 + monindex))
            .deathProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 18, plindex * 17 + 2 + monindex))
            .zeroMapper()
            .winProcessor((plindex, gameday) -> new StatLocation("Data", gameday + 6, plindex * 17 + 18))
            .looseProcessor((plindex, gameday) -> new StatLocation("Data", gameday + 18, plindex * 17 + 18))
            .resultCreator((DocEntry.BasicResultCreator) (b, gdi, index, numberOne, numberTwo, url) -> b.addSingle("Spielplan HR!%s%d".formatted(getAsXCoord(gdi * 9 + 5), index * 10 + 4),
                    "=HYPERLINK(\"%s\"; \"Link\")".formatted(url)))
            .setStatIfEmpty(false)
            .sorterData(new SorterData(List.of("Tabelle HR!C3:K8", "Tabelle HR!C12:K17"), List.of("Tabelle HR!D3:K8", "Tabelle HR!D12:K17"),
                    true, s -> Integer.parseInt(s.substring("=Data!F$".length())) / 17 - 1, 1, -1, 7));

    public static final DocEntry BSL = DocEntry.create()
            .leagueFunction((x, y) -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("BSL"))
            .onlyKilllist(() -> {
                try {
                    return Files.readAllLines(Paths.get("bslkilllist.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .setStatIfEmpty(false)
            .zeroMapper()
            .useProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 6, monindex + 2))
            .killProcessor((plindex, monindex, gameday) -> new StatLocation("Data", gameday + 18, monindex + 2));

    private DocEntries() {
    }
}
