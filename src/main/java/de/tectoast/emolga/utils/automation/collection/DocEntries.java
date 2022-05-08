package de.tectoast.emolga.utils.automation.collection;

import de.tectoast.emolga.utils.automation.structure.DocEntry;
import de.tectoast.emolga.utils.records.SorterData;
import de.tectoast.emolga.utils.records.StatLocation;

import java.util.Arrays;

import static de.tectoast.emolga.commands.Command.getAsXCoord;
import static de.tectoast.emolga.commands.Command.getEmolgaJSON;

public class DocEntries {
    public static final DocEntry UPL = DocEntry.create()
            .leagueFunction((x, y) -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("UPL"))
            .killProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 7, plindex * 12 + 2 + monindex))
            .deathProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 23, plindex * 12 + 2 + monindex))
            .useProcessor((plindex, monindex, gameday) -> new StatLocation("Stats", gameday + 15, plindex * 12 + 2 + monindex))
            .zeroMapper()
            .winProcessor((plindex, gameday) -> new StatLocation("Stats", "AF", plindex * 12 + 1 + gameday))
            .looseProcessor((plindex, gameday) -> new StatLocation("Stats", "AG", plindex * 12 + 1 + gameday))
            .resultCreator((b, gdi, index, numberOne, numberTwo, url) -> b.addRow("Spielplan!%s%d".formatted(getAsXCoord(gdi / 4 * 6 + 4), gdi % 4 * 6 + 6 + index), Arrays.asList(
                    numberOne, "=HYPERLINK(\"%s\"; \":\")".formatted(url), numberTwo
            )))
            .setStatIfEmpty(false)
            .sorterData(new SorterData("Tabelle!B2:H9", "Tabelle!B2:H9", false, null, 1, 4, 2));

    private DocEntries() {
    }
}
