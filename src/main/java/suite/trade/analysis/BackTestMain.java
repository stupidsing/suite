package suite.trade.analysis;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.Chars;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.BackAllocator_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Fun;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTester runner = new BackTester();
	private Configuration cfg = new ConfigurationImpl();

	private BackAllocConfigurations bacs = new BackAllocConfigurations(cfg, LogUtil::info);

	private Map<String, BackAllocConfiguration> bacByTag;

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;
		Map<String, BackAllocConfiguration> bacByTag_ = new HashMap<>();

		if (Boolean.FALSE) {
			bacByTag_.put("donchian9", BackAllocator_.donchian(9).cfgUnl(fun));
			questoaQuella("0020.HK", "0004.HK");
			questoaQuella("0052.HK", "0341.HK");
			questoaQuella("0670.HK", "1055.HK");
			questoaQuella("0753.HK", "1055.HK");
		}

		// BEGIN
		bacByTag_.put("hsi", bacs.bac_hsi);
		bacByTag_.put("bb", bacs.bac_bb);
		bacByTag_.put("donchian", bacs.bac_donchian);
		bacByTag_.put("ema", bacs.bac_ema);
		bacByTag_.put("facoil", bacs.bac_facoil);
		bacByTag_.put("lr", BackAllocator_.lastReturn(0, 2).cfgUnl(fun));
		bacByTag_.put("mix", bacs.bac_mix);
		bacByTag_.put("pmamr", bacs.bac_pmamr);
		bacByTag_.put("pmmmr", bacs.bac_pmmmr);
		bacByTag_.put("revco", bacs.bac_revco);
		bacByTag_.put("rsi", bacs.bac_rsi);
		bacByTag_.put("tma", bacs.bac_tma);
		// END

		Set<String> strategyNames = Read.from(args).toSet();

		bacByTag = Read //
				.from2(bacByTag_) //
				.filterKey(strategyName -> args.length == 0 || strategyNames.contains(strategyName)) //
				.toMap();

		Streamlet2<String, Simulate> simulationsByKey = Read //
				.from2(bacByTag) //
				.map(Pair::of) //
				.join2(IntStreamlet //
						.range(2008, Trade_.thisYear) //
						.map(TimeRange::ofYear)) //
				.map2((pair, period) -> pair.t0, (pair, period) -> {
					BackAllocConfiguration bac = pair.t1;
					Streamlet<Asset> assets = bac.assetsFun.apply(period.from);
					return runner.backTest(bac.backAllocator, period, assets);
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
		bacByTag.put(symbol0, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol0)));
		bacByTag.put(symbol1, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol1)));
		bacByTag.put("pair/" + symbol0 + "/" + symbol1, bacs.questoaQuella(symbol0, symbol1));
	}

}
