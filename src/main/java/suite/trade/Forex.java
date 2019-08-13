package suite.trade;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet2;

public class Forex {

	public Streamlet2<String, String> currencies = Read.<String, String> empty2() //
			.cons("AUDUSD=X", "AUD/USD") //
			.cons("EURUSD=X", "EUR/USD") //
			.cons("GBPUSD=X", "GBP/USD") //
			.cons("NZDUSD=X", "NZD/USD") //
			.collect();

	public Streamlet2<String, String> crossCurrencies = Read.<String, String> empty2() //
			.cons("EURCAD=X", "EUR/CAD") //
			.cons("EURCHF=X", "EUR/CHF") //
			.cons("EURGBP=X", "EUR/GBP") //
			.cons("EURHUF=X", "EUR/HUF") //
			.cons("EURJPY=X", "EUR/JPY") //
			.cons("GBPJPY=X", "GBP/JPY") //
			.cons("EURSEK=X", "EUR/SEK") //
			.collect();

	// require ^ -1
	public Streamlet2<String, String> invertedCurrencies = Read.<String, String> empty2() //
			.cons("CNY=X", "USD/CNY") //
			.cons("HKD=X", "USD/HKD") //
			.cons("IDR=X", "USD/IDR") //
			.cons("INR=X", "USD/INR") //
			.cons("JPY=X", "USD/JPY") //
			.cons("MXN=X", "USD/MXN") //
			.cons("MYR=X", "USD/MYR") //
			.cons("PHP=X", "USD/PHP") //
			.cons("RUB=X", "USD/RUB") //
			.cons("SGD=X", "USD/SGD") //
			.cons("THB=X", "USD/THB") //
			.cons("ZAR=X", "USD/ZAR") //
			.collect();

}
