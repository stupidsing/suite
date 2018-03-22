package suite.trade.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trade_;
import suite.util.FunUtil2.Fun2;

public class QuoteCache<K> {

	private Map<K, Map<String, Float>> quotesByField = new HashMap<>();
	private Fun2<Streamlet<String>, K, Map<String, Float>> quoteFun;

	public QuoteCache(Fun2<Streamlet<String>, K, Map<String, Float>> quoteFun) {
		this.quoteFun = quoteFun;
	}

	public synchronized Map<String, Float> quote(Set<String> symbols, K key) {
		Map<String, Float> quotes = quotesByField.computeIfAbsent(key, f -> new HashMap<>());
		Streamlet<String> querySymbols = Read //
				.from(symbols) //
				.filter(symbol -> !Trade_.isCacheQuotes || !quotes.containsKey(symbol)) //
				.distinct();
		quotes.putAll(quoteFun.apply(querySymbols, key));
		return Read.from(symbols).map2(quotes::get).toMap();
	}

}
