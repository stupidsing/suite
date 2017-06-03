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

	private Pair<Streamlet<Asset>, AssetAllocator> pair0;
	private Pair<Streamlet<Asset>, AssetAllocator> pair1;

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// BEGIN
		pair0 = Pair.of(Read.each(Asset.hsi), AssetAllocator_.ofSingle(Asset.hsiSymbol));
		pair1 = dm.pair_bb;

		questoaQuella("0670.HK", "1055.HK");
		questoaQuella("0052.HK", "0341.HK");
		questoaQuella("0004.HK", "0020.HK");

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					Pair<Streamlet<Asset>, AssetAllocator> pair = key ? pair1 : pair0;
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

	private void questoaQuella(String symbol0, String symbol1) {
		pair0 = pairOfSingle(symbol0);
		pair1 = dm.questoaQuella(symbol0, symbol1);
	}

	private Pair<Streamlet<Asset>, AssetAllocator> pairOfSingle(String symbol) {
		return Pair.of(Read.each(cfg.queryCompany(symbol)), AssetAllocator_.ofSingle(symbol));
	}

}
