package suite.trade.data;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Trade;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class QuoteTest {

	private Configuration configuration = new Configuration();
	private Summarize summarize = new Summarize(configuration);

	private Consumer<String> silent = s -> {
	};

	@Test
	public void testQuote() {
		System.out.println(configuration.quote(Read.each( //
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
		System.out.println(Read.from2(summarize(r -> r.symbol)) //
				.map((symbol, gain) -> symbol + " " + To.string(gain) + "\n") //
				.collect(As.joined()));
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
