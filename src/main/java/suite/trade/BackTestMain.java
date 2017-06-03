package suite.trade;

import java.nio.file.Paths;

import suite.Constants;
import suite.DailyMain;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocConfiguration;
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

	private AssetAllocConfiguration aac0;
	private AssetAllocConfiguration aac1;

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// BEGIN
		aac0 = new AssetAllocConfiguration(Read.each(Asset.hsi), AssetAllocator_.ofSingle(Asset.hsiSymbol));
		aac1 = dm.aac_bb;

		questoaQuella("0670.HK", "1055.HK");
		questoaQuella("0052.HK", "0341.HK");
		questoaQuella("0004.HK", "0020.HK");

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					AssetAllocConfiguration aac = key ? aac1 : aac0;
					Constants.testFlag = key;
					return runner.backTest(aac.assetAllocator, period, aac.assets);
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
		aac0 = pairOfSingle(symbol0);
		aac1 = dm.questoaQuella(symbol0, symbol1);
	}

	private AssetAllocConfiguration pairOfSingle(String symbol) {
		return new AssetAllocConfiguration(Read.each(cfg.queryCompany(symbol)), AssetAllocator_.ofSingle(symbol));
	}

}
