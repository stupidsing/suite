package suite.trade;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.TempDir;

public class QuoteTest {

	@Test
	public void testQuote() {
		Yahoo yahoo = new Yahoo();
		List<String[]> table = Read.bytes(TempDir.resolve("stock.txt")) //
				.collect(As::table) //
				.toList();
		Map<String, Integer> sizeByStockCodes = Read.from(table) //
				.map2(array -> array[2], array -> Integer.parseInt(array[1])) //
				.groupBy(sizes -> sizes.fold(0, (size0, size1) -> size0 + size1)) //
				.toMap();
		float amount0 = Read.from(table) //
				.map(array -> Integer.parseInt(array[1]) * Float.parseFloat(array[3])) //
				.fold(0f, (amt0, amt1) -> amt0 + amt1);
		Map<String, Float> priceByStockCodes = yahoo.quote(Read.from(sizeByStockCodes.keySet()));
		float amount1 = Read.from2(sizeByStockCodes) //
				.map((stockCode, size) -> priceByStockCodes.get(stockCode) * size) //
				.fold(0f, (amt0, amt1) -> amt0 + amt1);
		priceByStockCodes.forEach((stockCode, price) -> System.out.println(stockCode + " := " + price));
		System.out.println("AMOUNT0 = " + amount0);
		System.out.println("AMOUNT1 = " + amount1);
	}

}
