package suite.trade;

import java.time.LocalDate;

import suite.streamlet.Streamlet;
import suite.trade.assetalloc.AssetAllocBackTest;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocator;
import suite.trade.assetalloc.MovingAvgMeanReversionAssetAllocator0;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private float initial = 1000000f;
	private Sink<String> log = FunUtil.nullSink();
	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		LocalDate frDate = LocalDate.of(2016, 1, 1);
		LocalDate toDate = LocalDate.of(2017, 7, 1);
		DatePeriod period = DatePeriod.of(frDate, toDate);

		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator0.of(log);
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(frDate); // hkex.getCompanies()

		AssetAllocBackTest backTest = AssetAllocBackTest.ofFromTo( //
				cfg, //
				assets, //
				assetAllocator, //
				period, //
				log);

		Simulate sim = backTest.simulate(initial);

		System.out.println(sim.conclusion());
		System.out.println(sim.account.transactionSummary(cfg::transactionFee));
		return true;
	}

}
