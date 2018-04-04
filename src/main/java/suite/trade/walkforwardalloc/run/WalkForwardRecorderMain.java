package suite.trade.walkforwardalloc.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import suite.os.Schedule;
import suite.os.Scheduler;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.backalloc.strategy.BackAllocatorGeneral;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.HkexUtil;
import suite.trade.walkforwardalloc.WalkForwardAllocConfiguration;
import suite.trade.walkforwardalloc.WalkForwardAllocTester;
import suite.util.Fail;
import suite.util.HomeDir;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.WalkForwardRecorderMain
public class WalkForwardRecorderMain extends ExecutableProgram {

	private BackAllocatorGeneral bag = BackAllocatorGeneral.me;
	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		RunUtil.run(WalkForwardRecorderMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Streamlet<Asset> assets = cfg.queryCompaniesByMarketCap(Time.now());
		var fund0 = 1000000f;

		Trade_.isCacheQuotes = false;
		Trade_.isShortSell = true;
		Trade_.leverageAmount = fund0;

		if (Boolean.FALSE) { // record
			String ts = Time.now().ymdHms().replace("-", "").replace(" ", "-").replace(":", "");
			var filename = "wfa." + ts + ".csv";

			Schedule schedule = Schedule //
					.ofRepeat(5, () -> {
						var ymdHms = Time.now().ymdHms();
						Map<String, Float> priceBySymbol = cfg.quote(assets.map(asset -> asset.symbol).toSet());

						try (OutputStream os = Files.newOutputStream( //
								HomeDir.resolve(filename), //
								StandardOpenOption.APPEND, //
								StandardOpenOption.CREATE, //
								StandardOpenOption.WRITE); //
								PrintWriter bw = new PrintWriter(os)) {
							for (Entry<String, Float> e : priceBySymbol.entrySet())
								bw.println(ymdHms + ", " + e.getKey() + ", " + e.getValue());
						} catch (IOException ex) {
							Fail.t(ex);
						}
					});

			Scheduler.of(schedule.filterTime(dt -> HkexUtil.isMarketOpen(Time.of(dt)))).run();
		} else { // replay
			var ts = "20170612-092616";
			var filename = "wfa." + ts + ".csv";

			Map<Time, Map<String, Float>> data = new TreeMap<>();

			try (InputStream is = Files.newInputStream(HomeDir.resolve(filename)); //
					InputStreamReader isr = new InputStreamReader(is); //
					BufferedReader br = new BufferedReader(isr)) {
				while (br.ready()) {
					var array = br.readLine().split(",");
					Time time = Time.of(array[0].trim());
					var symbol = array[1].trim();
					var price = Float.parseFloat(array[2].trim());
					data.computeIfAbsent(time, s -> new HashMap<>()).put(symbol, price);
				}
			} catch (IOException ex) {
				Fail.t(ex);
			}

			WalkForwardAllocConfiguration wfac = new WalkForwardAllocConfiguration( //
					cfg.queryCompaniesByMarketCap(Time.now()), //
					bag.rsi.unleverage().walkForwardAllocator());

			WalkForwardAllocTester tester = new WalkForwardAllocTester(cfg, wfac.assets, fund0, wfac.walkForwardAllocator);

			for (Entry<Time, Map<String, Float>> e : data.entrySet())
				System.out.println(tester.tick(e.getKey(), e.getValue()));

			System.out.println(tester.conclusion());
		}

		return true;
	}

}
