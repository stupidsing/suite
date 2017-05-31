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
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTestRunner runner = new BackTestRunner();
	private DailyMain dm = new DailyMain();

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// BEGIN
		Pair<Streamlet<Asset>, AssetAllocator> pair;
		pair = dm.questaQuella("0341.HK", "0052.HK");
		pair = dm.pair_bb;

		Pair<Streamlet<Asset>, AssetAllocator> pair_ = pair;

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					AssetAllocator assetAllocator = key //
							? pair_.t1 //
							: AssetAllocator_.ofSingle("^HSI");

					Constants.testFlag = key;
					return runner.backTest(assetAllocator, period, pair_.t0);
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

}
