package suite.trade.walkforwardalloc.run;

import suite.os.Schedule;
import suite.os.Scheduler;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.backalloc.strategy.BackAllocatorGeneral;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.HkexUtil;
import suite.trade.walkforwardalloc.WalkForwardAllocConfiguration;
import suite.trade.walkforwardalloc.WalkForwardAllocTester;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.WalkForwardTestMain
public class WalkForwardTestMain extends ExecutableProgram {

	private BackAllocatorGeneral bag = BackAllocatorGeneral.me;
	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		RunUtil.run(WalkForwardTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		var fund0 = 1000000f;

		Trade_.isCacheQuotes = false;
		Trade_.isShortSell = true;
		Trade_.leverageAmount = fund0;

		WalkForwardAllocConfiguration wfac = new WalkForwardAllocConfiguration( //
				cfg.queryCompaniesByMarketCap(Time.now()), //
				bag.rsi.unleverage().walkForwardAllocator());

		WalkForwardAllocTester tester = new WalkForwardAllocTester(cfg, wfac.assets, fund0, wfac.walkForwardAllocator);

		Schedule schedule = Schedule //
				.ofRepeat(5, () -> System.out.println(tester.tick())) //
				.filterTime(dt -> HkexUtil.isMarketOpen(Time.of(dt)));

		Scheduler.of(schedule).run();

		return true;
	}

}
