package suite.trade;

import java.nio.file.Paths;

import suite.Constants;
import suite.DailyMain;
import suite.adt.pair.Pair;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocator;
import suite.trade.assetalloc.AssetAllocator_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTestRunner runner = new BackTestRunner();
	private Configuration cfg = new ConfigurationImpl();
	private DailyMain dm = new DailyMain();

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// BEGIN
		Pair<Streamlet<Asset>, AssetAllocator> pair0;
		Pair<Streamlet<Asset>, AssetAllocator> pair1;

		pair0 = Pair.of(Read.each(Asset.hsi), AssetAllocator_.ofSingle(Asset.hsiSymbol));
		pair1 = dm.pair_bb;

		pair0 = pairOfSingle("1055.HK");
		pair1 = dm.questaQuella("0670.HK", "1055.HK");

		pair0 = pairOfSingle("0052.HK");
		pair1 = dm.questaQuella("0052.HK", "0341.HK");

		Pair<Streamlet<Asset>, AssetAllocator> pair0_ = pair0;
		Pair<Streamlet<Asset>, AssetAllocator> pair1_ = pair1;

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					Pair<Streamlet<Asset>, AssetAllocator> pair = key ? pair1_ : pair0_;
					Constants.testFlag = key;
					return runner.backTest(pair.t1, period, pair.t0);
				}) //
				.collect(As::streamlet2);
		// END

		String content0 = Read.bytes(Paths.get("src/main/java/suite/trade/BackTestMain.java")) //
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

	private Pair<Streamlet<Asset>, AssetAllocator> pairOfSingle(String symbol) {
		return Pair.of(Read.each(cfg.queryCompany(symbol)), AssetAllocator_.ofSingle(symbol));
	}

}
