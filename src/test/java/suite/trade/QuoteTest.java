package suite.trade;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class QuoteTest {

	private Yahoo yahoo = new Yahoo();

	public class Record {
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

	@Test
	public void testQuote() {
		System.out.println(yahoo.quote(Read.each( //
				"0002.HK", //
				"0004.HK", //
				"0005.HK", //
				"0045.HK", //
				"0066.HK", //
				"0083.HK", //
				"0175.HK", //
				"0267.HK", //
				"0293.HK", //
				"0322.HK", //
				"1169.HK", //
				"1357.HK", //
				"2018.HK")));
	}

	@Test
	public void testQuoteManyStocks() {
		List<Record> table0 = Read.url("https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt") //
				.collect(As::table) //
				.map(Record::new) //
				.filter(r -> r.strategy.startsWith("mamr")) //
				.toList();

		Map<String, Integer> sizeByStockCodes = Read.from(table0) //
				.map2(r -> r.stockCode, r -> r.buySell) //
				.groupBy(sizes -> sizes.fold(0, (size0, size1) -> size0 + size1)) //
				.toMap();

		Map<String, Float> priceByStockCodes = yahoo.quote(Read.from(sizeByStockCodes.keySet()));

		List<Record> sellAll = Read.from2(sizeByStockCodes) //
				.map((stockCode, size) -> {
					float price = priceByStockCodes.get(stockCode);
					return new Record("-", -size, stockCode, price, "-");
				}) //
				.toList();

		List<Record> table1 = Streamlet.concat(Read.from(table0), Read.from(sellAll)).toList();

		float amount0 = Read.from(table0).map(r -> -r.buySell * r.price).collect(this::sum);
		float amount1 = Read.from(table1).map(r -> -r.buySell * r.price).collect(this::sum);

		System.out.println("CONSTITUENTS:");

		Read.from2(sizeByStockCodes) //
				.map((stockCode, size) -> stockCode + " := " + size + " * " + priceByStockCodes.get(stockCode)) //
				.forEach(System.out::println);

		System.out.println("AMOUNT0 = " + amount0);
		System.out.println("AMOUNT1 = " + amount1);
	}

	private float sum(Outlet<Float> outlet) {
		Source<Float> source = outlet.source();
		float result = 0f;
		Float f;
		while ((f = source.source()) != null)
			result += f;
		return result;
	}

}
