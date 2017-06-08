package suite.trade.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import suite.Constants;
import suite.DailyMain;
import suite.adt.pair.Pair;
import suite.os.Execute;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.DatePeriod;
import suite.trade.Time;
import suite.util.HomeDir;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class YahooHistory extends ExecutableProgram {

	private Yahoo yahoo = new Yahoo();

	private Path path = HomeDir.resolve("yahoo.history");
	private Map<String, Map<String, String>> data;

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) throws IOException {
		read();

		if (Boolean.TRUE) { // reload data for a symbol
			String symbol = "^HSI";
			DataSource dataSource = yahoo.dataSourceL1(symbol, DatePeriod.ages());
			Map<String, String> data_ = getDataBySymbol(read(), symbol);
			String[] dates = dataSource.dates;
			float[] prices = dataSource.prices;
			for (int i = 0; i < dates.length; i++)
				data_.put(dates[i], openLine(prices[i]));
			write();
		} else {
			String out = Execute.shell("find /home/ywsing/store-cache/ -type f | xargs grep -a -l table.csv");

			Streamlet2<String, String> keyValues = Read.from(out.split("\n")) //
					.concatMap(file -> {
						String[] array = To.string(Paths.get(file)).split("\n");
						String u = array[0];
						int p0 = u.indexOf("?s=");
						int p1 = u.indexOf("&", p0);
						String symbol = u.substring(p0 + 3, p1);
						return Read.from(array).drop(1).map(l -> symbol + "-" + l);
					}) //
					.map(l -> String_.split2(l, ",")) //
					.map2(p -> p.t0, p -> p.t1);

			Map<String, String> data = new TreeMap<>();
			StringBuilder sb = new StringBuilder();

			for (Pair<String, String> pair : keyValues)
				data.put(pair.t0, pair.t1);

			for (Entry<String, String> pair : data.entrySet())
				sb.append(pair.getKey().replace("%5E", "^") + "," + pair.getValue() + "\n");

			Files.write(path, sb.toString().getBytes(Constants.charset));
		}

		return true;
	}

	public DataSource dataSource(String symbol, DatePeriod period) {
		Map<String, Float> map = new TreeMap<>();
		String from = period.from.ymd();
		String to = period.to.ymd();

		for (Entry<String, String> e : read(symbol).entrySet()) {
			String date = e.getKey();
			String csv = e.getValue();
			String[] array = csv.split(",");
			String close = 3 < array.length ? array[3] : "-";
			// Date, Open, High, Low, Close, Volume, Adj Close

			if (!String_.equals(close, "-") && from.compareTo(date) <= 0 && date.compareTo(to) < 0)
				map.put(date, Float.parseFloat(close));
		}

		Streamlet2<String, Float> entries = Read.from2(map);
		String[] dates = entries.map((date, price) -> date).toArray(String.class);
		float[] prices = entries.map((date, price) -> price).collect(As.arrayOfFloats(price -> price));
		return new DataSource(dates, prices);
	}

	public boolean isContainsData(String symbol) {
		return read().containsKey(symbol);
	}

	private Map<String, String> read(String symbol) {
		return getDataBySymbol(read(), symbol);
	}

	private Map<String, Map<String, String>> read() {
		if (data == null) {
			data = new HashMap<>();

			for (String line : new String(Rethrow.ex(() -> Files.readAllBytes(path)), Constants.charset).split("\n")) {
				Pair<String, String> p0 = String_.split2(line, ",");
				Pair<String, String> p1 = String_.split2(p0.t0, "-");
				String symbol = p1.t0, date = p1.t1, csv = p0.t1;
				getDataBySymbol(data, symbol).put(date, csv);
			}

			// update after market closed
			Time now = Time.now();

			if (!HkexUtil.isMarketOpen(now)) {
				String date = HkexUtil.getTradeTimeBefore(now).date().ymd();
				Map<String, Float> quotes = yahoo.quote(data.keySet());

				for (Entry<String, Float> e : quotes.entrySet())
					getDataBySymbol(data, e.getKey()).put(date, openLine(e.getValue()));

				write();
			}
		}

		return data;
	}

	private String openLine(float open) {
		return "-,-,-," + To.string(open) + ",-,-";
	}

	private void write() {
		List<String> lines = new ArrayList<>();

		for (Entry<String, Map<String, String>> e0 : data.entrySet())
			for (Entry<String, String> e1 : e0.getValue().entrySet())
				lines.add(e0.getKey() + "-" + e1.getKey() + "," + e1.getValue());

		String text = Read.from(lines).map(line -> line + "\n").sort(Object_::compare).collect(As.joined());

		try {
			Files.write(path, text.getBytes(Constants.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Map<String, String> getDataBySymbol(Map<String, Map<String, String>> data, String symbol) {
		return data.computeIfAbsent(symbol, symbol_ -> new HashMap<>());
	}

}
