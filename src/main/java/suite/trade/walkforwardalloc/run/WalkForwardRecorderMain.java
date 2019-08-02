package suite.trade.walkforwardalloc.run;

import static primal.statics.Fail.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import primal.Verbs.ReadFile;
import suite.cfg.HomeDir;
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

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.WalkForwardRecorderMain
public class WalkForwardRecorderMain {

	private BackAllocatorGeneral bag = BackAllocatorGeneral.me;
	private TradeCfg cfg = new TradeCfgImpl();

	public static void main(String[] args) {
		RunUtil.run(() -> new WalkForwardRecorderMain().run());
	}

	private boolean run() {
		var instruments = cfg.queryCompaniesByMarketCap(Time.now());
		var fund0 = 1000000f;

		Trade_.isCacheQuotes = false;
		Trade_.isShortSell = true;
		Trade_.leverageAmount = fund0;

		if (Boolean.FALSE) { // record
			var ts = Time.now().ymdHms().replace("-", "").replace(" ", "-").replace(":", "");
			var filename = "wfa." + ts + ".csv";

			var schedule = Schedule //
					.ofRepeat(5, () -> {
						var ymdHms = Time.now().ymdHms();
						var priceBySymbol = cfg.quote(instruments.map(instrument -> instrument.symbol).toSet());

						try (var os = Files.newOutputStream(HomeDir.resolve(filename), //
								StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE); //
								var bw = new PrintWriter(os)) {
							for (var e : priceBySymbol.entrySet())
								bw.println(ymdHms + ", " + e.getKey() + ", " + e.getValue());
						} catch (IOException ex) {
							fail(ex);
						}
					});

			Scheduler.of(schedule.filterTime(dt -> HkexUtil.isMarketOpen(Time.of(dt)))).run();
		} else { // replay
			var ts = "20170612-092616";
			var filename = "wfa." + ts + ".csv";

			var data = ReadFile.from(HomeDir.resolve(filename)).doBufferedReader(br -> {
				var data_ = new TreeMap<Time, Map<String, Float>>();
				while (br.ready()) {
					var array = br.readLine().split(",");
					var time = Time.of(array[0].trim());
					var symbol = array[1].trim();
					var price = Float.parseFloat(array[2].trim());
					data_.computeIfAbsent(time, s -> new HashMap<>()).put(symbol, price);
				}
				return data_;
			});

			var wfac = new WalkForwardAllocConfiguration( //
					cfg.queryCompaniesByMarketCap(Time.now()), //
					bag.rsi.unleverage().walkForwardAllocator());

			var tester = new WalkForwardAllocTester(cfg, wfac.instruments, fund0, wfac.walkForwardAllocator);

			for (var e : data.entrySet())
				System.out.println(tester.tick(e.getKey(), e.getValue()));

			System.out.println(tester.conclusion());
		}

		return true;
	}

}
