package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import suite.streamlet.As;
import suite.streamlet.Read;

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

}
