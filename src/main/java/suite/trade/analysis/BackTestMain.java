package suite.trade.analysis;

import java.nio.file.Paths;

import suite.Constants;
import suite.DailyMain;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocator_;
import suite.trade.backalloc.BackAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTester runner = new BackTester();
	private Configuration cfg = new ConfigurationImpl();
	private DailyMain dm = new DailyMain();

	private BackAllocConfiguration aac0;
	private BackAllocConfiguration aac1;

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		BackAllocConfiguration aac_hsi = new BackAllocConfiguration( //
				Read.each(Asset.hsi), //
				BackAllocator_.ofSingle(Asset.hsiSymbol));

		aac0 = aac_hsi;
		aac1 = dm.aac_bb;

		questoaQuella("0670.HK", "1055.HK");
		questoaQuella("0052.HK", "0341.HK");
		questoaQuella("0020.HK", "0004.HK");

		aac0 = aac_hsi;
		aac1 = dm.assetAllocConfigurationOf(BackAllocator_.threeMovingAvgs());

		aac0 = aac_hsi;
		aac1 = dm.assetAllocConfigurationOf(BackAllocator_.variableBollingerBands());

		// BEGIN
		aac0 = aac_hsi;
		aac1 = dm.aac_pmmmr;
		// END

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					BackAllocConfiguration aac = key ? aac1 : aac0;
					Constants.testFlag = key;
					return runner.backTest(aac.backAllocator, period, aac.assets);
				}) //
				.collect(As::streamlet2);

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

	private BackAllocConfiguration pairOfSingle(String symbol) {
		return new BackAllocConfiguration(Read.each(cfg.queryCompany(symbol)), BackAllocator_.ofSingle(symbol));
	}

}
