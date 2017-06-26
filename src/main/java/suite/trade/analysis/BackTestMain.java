package suite.trade.analysis;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import suite.DailyMain;
import suite.adt.pair.Pair;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.IntStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTester runner = new BackTester();
	private Configuration cfg = new ConfigurationImpl();
	private DailyMain dm = new DailyMain();

	private Map<String, BackAllocConfiguration> bacs = new HashMap<>();

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		BackAllocConfiguration bac_hsi = BackAllocConfiguration.ofSingle(Asset.hsi);

		if (Boolean.FALSE) {
			questoaQuella("0670.HK", "1055.HK");
			questoaQuella("0052.HK", "0341.HK");
			questoaQuella("0020.HK", "0004.HK");

			bacs.put("hsi", bac_hsi);
		}

		// BEGIN
		bacs.put("hsi", bac_hsi);
		bacs.put("bb", dm.bac_bb);
		bacs.put("donchian", dm.bac_donchian);
		bacs.put("pmamr", dm.bac_pmamr);
		bacs.put("pmmmr", dm.bac_pmmmr);
		bacs.put("tma", dm.bac_tma);
		questoaQuella("0753.HK", "1055.HK");
		// END

		Streamlet2<String, Simulate> simulationsByKey = Read //
				.from2(bacs) //
				.map(Pair::of) //
				.join2(IntStreamlet.range(2008, 2018).map(TimeRange::ofYear)) //
				.map2((pair, period) -> pair.t0, (pair, period) -> {
					BackAllocConfiguration bac = pair.t1;
					return runner.backTest(bac.backAllocator, period, bac.assets);
				}) //
				.collect(As::streamlet2);

		String content0 = Read.bytes(Paths.get("src/main/java/suite/trade/analysis/BackTestMain.java")) //
				.collect(As::utf8decode) //
				.map(Chars::toString) //
				.collect(As.joined());

		int p0 = 0;
		int p1 = 0 <= p0 ? content0.indexOf("// BEGIN", p0) + 8 : -1;
		int p2 = 0 <= p1 ? content0.indexOf("// END", p1) : -1;
		String content1 = content0.substring(p1, p2);

		System.out.println(content1);
		System.out.println(runner.conclude(simulationsByKey));

		return true;
	}

	private void questoaQuella(String symbol0, String symbol1) {
		bacs.put(symbol0, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol0)));
		bacs.put(symbol1, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol1)));
		bacs.put("pair/" + symbol0 + "/" + symbol1, dm.questoaQuella(symbol0, symbol1));
	}

}
