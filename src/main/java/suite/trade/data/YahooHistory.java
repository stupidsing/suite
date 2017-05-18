package suite.trade.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import suite.Constants;
import suite.adt.Pair;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.DatePeriod;
import suite.util.HomeDir;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;

public class YahooHistory {

	private Yahoo yahoo = new Yahoo();

	private Path path = HomeDir.resolve("yahoo.history");
	private Map<String, Map<String, String>> data;

	public static void main(String[] args) throws IOException {
		new YahooHistory().init();
	}

	private void init() throws IOException {
		if (Boolean.TRUE)
			read();
		else {
			String out = Execute.shell("find /home/ywsing/store-cache/ -type f | xargs grep -a -l table.csv");

			Streamlet2<String, String> keyValues = Read.from(out.split("\n")) //
					.concatMap(file -> {
						String[] array = Rethrow.ex(() -> FileUtil.read(Paths.get(file))).split("\n");
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
	}

	public DataSource dataSource(String symbol, DatePeriod period) {
		Map<String, Float> map = new TreeMap<>();
		String from = To.string(period.from);
		String to = To.string(period.to);

		for (Entry<String, String> e : read().get(symbol).entrySet()) {
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

	private Map<String, Map<String, String>> read() {
		if (data == null) {
			data = new HashMap<>();

			for (String line : new String(Rethrow.ex(() -> Files.readAllBytes(path)), Constants.charset).split("\n")) {
				Pair<String, String> p0 = String_.split2(line, ",");
				Pair<String, String> p1 = String_.split2(p0.t0, "-");
				String symbol = p1.t0, date = p1.t1, csv = p0.t1;
				getDataBySymbol(symbol).put(date, csv);
			}

			// update after market closed
			LocalDateTime now = LocalDateTime.now();

			if (!HkexUtil.isMarketOpen(now)) {
				String date = To.string(HkexUtil.getPreviousTradeDate(now));
				Map<String, Float> quotes = yahoo.quote(data.keySet());

				for (Entry<String, Float> e : quotes.entrySet())
					getDataBySymbol(e.getKey()).put(date, "-,-,-," + To.string(e.getValue()) + ",-,-");

				write();
			}
		}

		return data;
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

	private Map<String, String> getDataBySymbol(String symbol) {
		return data.computeIfAbsent(symbol, symbol_ -> new HashMap<>());
	}

}
