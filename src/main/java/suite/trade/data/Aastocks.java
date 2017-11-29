package suite.trade.data;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.http.HttpUtil;
import suite.primitive.Chars_;
import suite.primitive.Ints_;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.util.To;

public class Aastocks {

	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	public float hsi() {
		String urlString = "http://www.aastocks.com/en/mobile/Quote.aspx?symbol=00005";
		URL url = To.url(urlString);
		List<String> lines = HttpUtil.get(url).out.collect(As::lines).toList();
		int i0 = Ints_.range(lines.size()).filter(i -> lines.get(i).contains("HSI")).first();
		return Float.parseFloat(new String(Chars_ //
				.of(lines.get(i0 + 1).toCharArray()) //
				.filter(c -> '0' <= c && c <= '9') //
				.toArray()));
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return quote(symbols, "l1"); // last price
		// "o" - open
	}

	private Map<String, Float> quote(Set<String> symbols, String field) {
		return quoteCache.quote(symbols, field);
	}

	private Map<String, Float> quote_(Streamlet<String> symbols, String field) {
		return symbols.map2(symbol -> {
			String urlString = "http://www.aastocks.com/en/mobile/Quote.aspx?symbol=0" + symbol.substring(0, 4);
			URL url = To.url(urlString);
			List<String> lines = HttpUtil.get(url).out.collect(As::lines).toList();
			int i0 = Ints_.range(lines.size()).filter(i -> lines.get(i).contains("text_last")).first();
			return Float.parseFloat(new String(Chars_ //
					.of(lines.get(i0 + 1).replace("0px", "").replace(".png", "").toCharArray()) //
					.filter(c -> c == '.' || '0' <= c && c <= '9') //
					.toArray()));
		}).toMap();
	}

}
