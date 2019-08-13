package suite.trade.data;

import java.util.Map;

import org.junit.Test;

import primal.MoreVerbs.Read;
import primal.fp.Funs.Fun;
import suite.trade.Trade;
import suite.trade.analysis.Summarize;
import suite.util.To;

public class QuoteTest {

	private TradeCfg cfg = new TradeCfgImpl();
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
		System.out.println(Read //
				.from2(summarizeOut(r -> r.symbol)) //
				.sortBy((symbol, gain) -> -gain) //
				.map((symbol, gain) -> symbol + " " + To.string(gain)) //
				.toLines());
	}

	@Test
	public void testQuotesByStrategies() {
		System.out.println(summarizeOut(r -> r.strategy));
	}

	private Map<String, Double> summarizeOut(Fun<Trade, String> fun) {
		var sbs = summarize.summarize(fun);
		System.out.println(sbs.log);
		return sbs.pnlByKey;
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun) {
		return summarize.summarize(fun).pnlByKey;
	}

}
