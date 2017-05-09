package suite.trade.data;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.util.FunUtil.Fun;

public class QuoteTest {

	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();
	private Fun<Set<String>, Map<String, Float>> quoteFun = yahoo::quote;
	private Fun<String, Asset> getAssetFun = hkex::getCompany;
	private Summarize summarize = new Summarize(quoteFun, getAssetFun);

	private Consumer<String> silent = s -> {
	};

	@Test
	public void testQuote() {
		System.out.println(quoteFun.apply(Read.each( //
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
				"2018.HK").toSet()));
	}

	@Test
	public void testQuotes() {
		System.out.println("P/L = " + summarize(r -> "HKEX", silent));
	}

	@Test
	public void testQuotesDetail() {
		summarize(r -> "HKEX");
	}

	@Test
	public void testQuotesByStock() {
		System.out.println(summarize(r -> r.stockCode));
	}

	@Test
	public void testQuotesByStrategies() {
		System.out.println(summarize(r -> r.strategy, silent));
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun) {
		return summarize(fun, System.out::println);
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun, Consumer<String> log) {
		return summarize.summarize(fun, log);
	}

}
