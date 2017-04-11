package suite.trade;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;

public class Forex {

	public Streamlet2<String, String> currencies = Read.<String, String> empty2() //
			.cons("EURUSD=X", "EUR/USD") //
			.cons("GBPUSD=X", "GBP/USD") //
			.cons("AUDUSD=X", "AUD/USD") //
			.cons("NZDUSD=X", "NZD/USD") //
			.collect(As::streamlet2);

	public Streamlet2<String, String> crossCurrencies = Read.<String, String> empty2() //
			.cons("EURJPY=X", "EUR/JPY") //
			.cons("GBPJPY=X", "GBP/JPY") //
			.cons("EURGBP=X", "EUR/GBP") //
			.cons("EURCAD=X", "EUR/CAD") //
			.cons("EURSEK=X", "EUR/SEK") //
			.cons("EURCHF=X", "EUR/CHF") //
			.cons("EURHUF=X", "EUR/HUF") //
			.collect(As::streamlet2);

	// require ^ -1
	public Streamlet2<String, String> invertedCurrencies = Read.<String, String> empty2() //
			.cons("JPY=X", "USD/JPY") //
			.cons("CNY=X", "USD/CNY") //
			.cons("HKD=X", "USD/HKD") //
			.cons("SGD=X", "USD/SGD") //
			.cons("INR=X", "USD/INR") //
			.cons("MXN=X", "USD/MXN") //
			.cons("PHP=X", "USD/PHP") //
			.cons("IDR=X", "USD/IDR") //
			.cons("THB=X", "USD/THB") //
			.cons("MYR=X", "USD/MYR") //
			.cons("ZAR=X", "USD/ZAR") //
			.cons("RUB=X", "USD/RUB") //
			.collect(As::streamlet2);

}
