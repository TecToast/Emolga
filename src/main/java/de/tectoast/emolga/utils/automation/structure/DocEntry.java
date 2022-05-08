package de.tectoast.emolga.utils.automation.structure;

import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.records.SorterData;
import de.tectoast.emolga.utils.records.StatLocation;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.Pokemon;
import de.tectoast.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static de.tectoast.emolga.commands.Command.*;

public class DocEntry {

    private static final Logger logger = LoggerFactory.getLogger(DocEntry.class);

    private static final StatProcessor invalidProcessor = (plindex, monindex, gameday) -> StatLocation.invalid();
    private final BiFunction<JSONObject, Long, Integer> tableIndex = (o, u) -> o.getLongList("table").indexOf(u);
    private BiFunction<Long, Long, JSONObject> leagueFunction;
    private StatProcessor killProcessor = invalidProcessor;
    private StatProcessor deathProcessor = invalidProcessor;
    private StatProcessor useProcessor = invalidProcessor;
    private ResultStatProcessor winProcessor;
    private ResultStatProcessor looseProcessor;
    private ResultCreator resultCreator;
    private SorterData sorterData;
    private boolean setStatIfEmpty;
    private Function<String, String> numberMapper;

    private DocEntry() {
    }

    public static DocEntry create() {
        return new DocEntry();
    }

    public void execute(Player[] game, long uid1, long uid2, List<Map<String, String>> kills, List<Map<String, String>> deaths, Object[] optionalArgs) {
        JSONObject league = leagueFunction.apply(uid1, uid2);
        String sid = league.getString("sid");
        RequestBuilder b = new RequestBuilder(sid);
        int gameday = getGameDay(league, uid1, uid2);
        int i = 0;
        List<Long> uids = List.of(uid1, uid2);
        for (long uid : uids) {
            int index = tableIndex.apply(league, uid);
            List<String> picks = getPicksAsList(league.getJSONObject("picks").getJSONArray(uid));
            int monIndex = -1;
            for (String pick : picks) {
                monIndex++;
                String death = getNumber(deaths.get(i), pick);
                if (death.isEmpty() && !setStatIfEmpty) continue;
                StatLocation k = killProcessor.process(index, monIndex, gameday);
                if (k.isValid())
                    b.addSingle("%s!%s%d".formatted(k.sheet(), k.x(), k.y()), numberMapper.apply(getNumber(kills.get(i), pick)));
                StatLocation d = deathProcessor.process(index, monIndex, gameday);
                if (d.isValid())
                    b.addSingle("%s!%s%d".formatted(d.sheet(), d.x(), d.y()), numberMapper.apply(death));
                StatLocation u = useProcessor.process(index, monIndex, gameday);
                if (u.isValid())
                    b.addSingle("%s!%s%d".formatted(u.sheet(), u.x(), u.y()), numberMapper.apply("1"));
            }
            StatLocation w = (game[i].isWinner() ? winProcessor : looseProcessor).process(index, gameday);
            b.addSingle("%s!%s%d".formatted(w.sheet(), w.x(), w.y()), 1);
            if (game[i].isWinner()) {
                league.createOrGetJSON("results").put(uid1 + ":" + uid2, uid);
            }
            i++;
        }
        List<String> battleorder = Arrays.asList(league.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";"));
        int battleindex = IntStream.range(0, battleorder.size()).filter(x -> battleorder.get(x).contains(String.valueOf(uid1))).findFirst().orElse(-1);
        String battle = battleorder.stream().filter(x -> x.contains(String.valueOf(uid1))).findFirst().orElse("");
        List<String> battleusers = Arrays.asList(battle.split(":"));
        List<Integer> numbers = IntStream.range(0, 2)
                .boxed()
                .sorted(Comparator.comparing(o -> battleusers.indexOf(String.valueOf(uids.get(o)))))
                .map(x -> game[x].getMons().stream().filter(Predicate.not(Pokemon::isDead)).mapToInt(p -> 1).sum())
                .toList();
        resultCreator.process(b, gameday - 1, battleindex, numbers.get(0), numbers.get(1), (String) optionalArgs[1]);
        b.withRunnable(() -> sort(sid, league), 3000).suppressMessages().execute();
    }

    private void sort(String sid, JSONObject league) {
        String formulaRange = sorterData.formulaRange();
        List<List<Object>> formula = Google.get(sid, formulaRange, true, false);
        List<List<Object>> points = Google.get(sid, sorterData.pointRange(), false, false);
        List<List<Object>> orig = new ArrayList<>(points);
        List<Long> table = league.getLongList("table");
        points.sort((o1, o2) -> {
            List<Integer> arr = Arrays.stream(sorterData.cols()).boxed().toList();
            List<Integer> first = sorterData.directCompare() ? arr.subList(0, arr.indexOf(-1)) : arr;
            int c = compareColumns(o1, o2, first.stream().mapToInt(i -> i).toArray());
            if (c != 0) return c;
            if (!sorterData.directCompare()) return 0;
            long u1 = table.get(sorterData.indexer().apply(String.valueOf(formula.get(orig.indexOf(o1)).get(0))));
            long u2 = table.get(sorterData.indexer().apply(String.valueOf(formula.get(orig.indexOf(o1)).get(0))));
            if (league.has("results")) {
                JSONObject o = league.getJSONObject("results");
                if (o.has(u1 + ":" + u2)) {
                    return o.getLong(u1 + ":" + u2) == u1 ? 1 : -1;
                }
                if (o.has(u2 + ":" + u1)) {
                    return o.getLong(u2 + ":" + u1) == u1 ? 1 : -1;
                }
            }
            List<Integer> second = arr.subList(arr.indexOf(-1), arr.size());
            if (second.size() > 1)
                return compareColumns(o1, o2, second.stream().skip(1).mapToInt(i -> i).toArray());
            return 0;
        });
        Collections.reverse(points);
        //logger.info(points);
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        int i = 0;
        for (List<Object> objects : orig) {
            namap.put(points.indexOf(objects), formula.get(i));
            i++;
        }
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < points.size(); j++) {
            sendname.add(namap.get(j));
        }
        RequestBuilder.updateAll(sid, formulaRange.substring(0, formulaRange.indexOf(":")), sendname);
    }

    public DocEntry leagueFunction(BiFunction<Long, Long, JSONObject> leagueFunction) {
        this.leagueFunction = leagueFunction;
        return this;
    }

    public DocEntry numberMapper(Function<String, String> numberMapper) {
        this.numberMapper = numberMapper;
        return this;
    }

    public DocEntry zeroMapper() {
        return numberMapper(s -> s.isEmpty() ? "0" : s);
    }

    public DocEntry hyphenMapper() {
        return numberMapper(s -> s.isEmpty() ? "-" : s);
    }

    public DocEntry killProcessor(StatProcessor killProcessor) {
        this.killProcessor = killProcessor;
        return this;
    }

    public DocEntry deathProcessor(StatProcessor deathProcessor) {
        this.deathProcessor = deathProcessor;
        return this;
    }

    public DocEntry useProcessor(StatProcessor useProcessor) {
        this.useProcessor = useProcessor;
        return this;
    }

    public DocEntry winProcessor(ResultStatProcessor winProcessor) {
        this.winProcessor = winProcessor;
        return this;
    }

    public DocEntry looseProcessor(ResultStatProcessor looseProcessor) {
        this.looseProcessor = looseProcessor;
        return this;
    }

    public DocEntry resultCreator(ResultCreator resultCreator) {
        this.resultCreator = resultCreator;
        return this;
    }

    public DocEntry sorterData(SorterData sorterData) {
        this.sorterData = sorterData;
        return this;
    }

    public DocEntry setStatIfEmpty(boolean setStatIfEmpty) {
        this.setStatIfEmpty = setStatIfEmpty;
        return this;
    }

    @FunctionalInterface
    public interface StatProcessor {
        StatLocation process(int plindex, int monindex, int gameday);
    }

    @FunctionalInterface
    public interface ResultStatProcessor {
        StatLocation process(int plindex, int gameday);
    }

    @FunctionalInterface
    public interface ResultCreator {
        void process(RequestBuilder b, int gdi, int index, int numberOne, int numberTwo, String url);
    }

}
