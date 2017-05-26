package suite.trade;

import java.nio.file.Paths;

import suite.Constants;
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

	private Configuration cfg = new ConfigurationImpl();
	private BackTestRunner runner = new BackTestRunner();

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// BEGIN
		String symbol0 = "0945.HK";
		String symbol1 = "1299.HK";
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					AssetAllocator assetAllocator = key //
							? AssetAllocator_.questoQuella(symbol0, symbol1) //
							: AssetAllocator_.ofSingle("^HSI");

					Constants.testFlag = key;
					return runner.backTest(assetAllocator, period, assets);
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
