package suite.trade.walkforwardalloc.run;

import suite.os.Schedule;
import suite.os.Scheduler;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.backalloc.strategy.BackAllocatorGeneral;
import suite.trade.data.HkexUtil;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.trade.walkforwardalloc.WalkForwardAllocConfiguration;
import suite.trade.walkforwardalloc.WalkForwardAllocTester;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.WalkForwardTestMain
public class WalkForwardTestMain {

	private BackAllocatorGeneral bag = BackAllocatorGeneral.me;
	private TradeCfg cfg = new TradeCfgImpl();

	public static void main(String[] args) {
		RunUtil.run(new WalkForwardTestMain()::run);
	}

	private boolean run() {
		var fund0 = 1000000f;

		Trade_.isCacheQuotes = false;
		Trade_.isShortSell = true;
		Trade_.leverageAmount = fund0;

		var wfac = new WalkForwardAllocConfiguration(
				cfg.queryCompaniesByMarketCap(Time.now()),
				bag.rsi.unleverage().walkForwardAllocator());

		var tester = new WalkForwardAllocTester(cfg, wfac.instruments, fund0, wfac.walkForwardAllocator);

		var schedule = Schedule
				.ofRepeat(5, () -> System.out.println(tester.tick()))
				.filterTime(dt -> HkexUtil.isMarketOpen(Time.of(dt)));

		Scheduler.of(schedule).run();

		return true;
	}

}
