package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Util;

public class Trans {

	public static class Record {
		public String date;
		public int buySell;
		public String stockCode;
		public float price;
		public String strategy;

		public Record(String[] array) {
			this(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
		}

		public Record(String date, int buySell, String stockCode, float price, String strategy) {
			this.date = date;
			this.buySell = buySell;
			this.stockCode = stockCode;
			this.price = price;
			this.strategy = strategy;
		}
	}

	public static List<Record> fromHistory(Predicate<Record> pred) {
		return Read.url("https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt") //
				.collect(As::table) //
				.map(Record::new) //
				.filter(pred) //
				.toList();
	}

	public static Map<String, Integer> portfolio(List<Record> records) {
		return Read.from(records) //
				.map2(r -> r.stockCode, r -> r.buySell) //
				.groupBy(sizes -> sizes.collect(As.sumOfInts(size -> size))) //
				.filterValue(size -> size != 0) //
				.toMap();
	}

	// Profit & loss
	public static float returns(List<Record> records) {
		return Read.from(records).collect(As.sumOfFloats(r -> -r.buySell * r.price));
	}

	public static List<Pair<String, Integer>> diff(Map<String, Integer> assets0, Map<String, Integer> assets1) {
		Set<String> stockCodes = Streamlet2.concat(Read.from2(assets0), Read.from2(assets1)) //
				.map((stockCode, nShares) -> stockCode) //
				.toSet();

		return Read.from(stockCodes) //
				.map2(stockCode -> {
					int n0 = assets0.computeIfAbsent(stockCode, s -> 0);
					int n1 = assets1.computeIfAbsent(stockCode, s -> 0);
					return n1 - n0;
				}) //
				.filter((stockCode, n) -> !Util.stringEquals(stockCode, Asset.cash.code)) //
				.toList();
	}

}
