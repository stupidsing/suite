package suite.trade.analysis;

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
import suite.trade.backalloc.BackAllocator_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.HkexUtil;
import suite.trade.walkforwardalloc.WalkForwardAllocConfiguration;
import suite.trade.walkforwardalloc.WalkForwardAllocTester;
import suite.util.HomeDir;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.WalkForwardTestMain
public class WalkForwardRecorderMain extends ExecutableProgram {

	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		Util.run(WalkForwardRecorderMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(Time.now());

		Trade_.isCacheQuotes = false;

		if (Boolean.TRUE) { // record
			Schedule schedule = Schedule //
					.ofRepeat(5, () -> {
						String ymdHms = Time.now().ymdHms();
						Map<String, Float> priceBySymbol = cfg.quote(assets.map(asset -> asset.symbol).toSet());

						try (OutputStream os = Files.newOutputStream( //
								HomeDir.resolve("wfa.csv"), //
								StandardOpenOption.APPEND, StandardOpenOption.WRITE); //
								PrintWriter bw = new PrintWriter(os)) {
							for (Entry<String, Float> e : priceBySymbol.entrySet())
								bw.println(ymdHms + ", " + e.getKey() + ", " + e.getValue());
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					});

			Scheduler.of(schedule.filterTime(dt -> HkexUtil.isMarketOpen(Time.of(dt)))).run();
		} else { // replay
			Map<String, Map<String, Float>> data = new TreeMap<>();

			try (InputStream is = Files.newInputStream(HomeDir.resolve("wfa.csv")); //
					InputStreamReader isr = new InputStreamReader(is); //
					BufferedReader br = new BufferedReader(isr)) {
				while (br.ready()) {
					String[] array = br.readLine().split(",");
					data.computeIfAbsent(array[0], s -> new HashMap<>()).put(array[1], Float.parseFloat(array[2]));
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			WalkForwardAllocConfiguration wfac = new WalkForwardAllocConfiguration( //
					cfg.queryLeadingCompaniesByMarketCap(Time.now()), //
					BackAllocator_.rsi().walkForwardAllocator());

			float fund0 = 1000000f;
			WalkForwardAllocTester tester = new WalkForwardAllocTester(cfg, wfac.assets, fund0, wfac.walkForwardAllocator);

			for (Entry<String, Map<String, Float>> e : data.entrySet())
				tester.tick(e.getKey(), e.getValue());
		}

		return true;
	}

}
