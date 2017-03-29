package suite.trade;

import java.time.LocalDate;
import java.util.List;

import suite.Constants;
import suite.os.StoreCache;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Read;

public class DataSource {

	public final String dates[];
	public final double prices[];

	public static DataSource yahoo(String stockCode, LocalDate frDate, LocalDate toDate) {
		String urlString = "http://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";

		// Date, Open, High, Low, Close, Volume, Adj Close
		List<String[]> arrays = new StoreCache() //
				.http(urlString) //
				.collect(BytesUtil.split(Bytes.of((byte) 10))) //
				.skip(1) //
				.map(bytes -> new String(bytes.toBytes(), Constants.charset).split(",")) //
				.sort((a0, a1) -> a0[0].compareTo(a1[0])) //
				.toList();

		String dates[] = Read.from(arrays) //
				.map(array -> array[0]) //
				.toArray(String.class);

		double prices[] = Read.from(arrays) //
				.collect(As.arrayOfDoubles(array -> Double.parseDouble(array[1])));

		return new DataSource(dates, prices);
	}

	private DataSource(String dates[], double prices[]) {
		this.dates = dates;
		this.prices = prices;
	}

}
