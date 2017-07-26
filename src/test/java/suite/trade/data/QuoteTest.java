package suite.trade.data;

import java.util.Map;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Trade;
import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class QuoteTest {

	private Configuration cfg = new ConfigurationImpl();
	private Summarize summarize = Summarize.of(cfg);

	@Test
	public void testQuote() {
		System.out.println(cfg.quote(Read.each( //
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
		System.out.println("P/L = " + summarize(r -> "HKEX"));
	}

	@Test
	public void testQuotesDetail() {
		summarizeOut(r -> "HKEX");
	}

	@Test
	public void testQuotesByStock() {
		System.out.println(Read.from2(summarizeOut(r -> r.symbol)) //
				.sortBy((symbol, gain) -> -gain) //
				.map((symbol, gain) -> symbol + " " + To.string(gain) + "\n") //
				.collect(As.joined()));
	}

	@Test
	public void testQuotesByStrategies() {
		System.out.println(summarizeOut(r -> r.strategy));
	}

	private Map<String, Double> summarizeOut(Fun<Trade, String> fun) {
		SummarizeByStrategy<String> byStrategy = summarize.out(fun);
		System.out.println(byStrategy.log);
		return byStrategy.pnlByKey;
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun) {
		return summarize.out(fun).pnlByKey;
	}

}
