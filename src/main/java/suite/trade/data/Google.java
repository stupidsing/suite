package suite.trade.data;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.http.HttpUtil;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trade_;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class Google {

	private static ObjectMapper mapper = new ObjectMapper();

	// http://www.jarloo.com/real-time-google-stock-api/
	public Map<String, Float> quote(Set<String> symbols) {

		// This must be queried separately, or server would return JWNG of
		// London Exchange. Do not know why.
		if (symbols.contains("2020.HK"))
			quotesBySymbol.putAll(quote_(Collections.singleton("2020.HK")));
		return quote_(symbols);
	}

	private Map<String, Float> quote_(Set<String> symbols) {
		Streamlet<String> querySymbols = Read.from(symbols) //
				.filter(symbol -> !Trade_.isCacheQuotes || !quotesBySymbol.containsKey(symbol)) //
				.distinct();
		quotesBySymbol.putAll(querySymbols //
				.chunk(100) //
				.map(Outlet::toList) //
				.concatMap2(symbols_ -> Read.from2(quote_(Read.from(symbols_)))) //
				.toMap());
		return Read.from(symbols).map2(quotesBySymbol::get).toMap();
	}

	private static Map<String, Float> quotesBySymbol = new HashMap<>();

	private synchronized Map<String, Float> quote_(Streamlet<String> symbols) {
		if (0 < symbols.size()) {
			URL url = To.url("http://finance.google.com/finance/info?client=ig&q=HKEX%3A" //
					+ symbols.sort(Object_::compare).map(this::fromSymbol).collect(As.joined(",")));

			JsonNode json = Rethrow.ex(() -> {
				try (InputStream is = HttpUtil.get(url).out.collect(To::inputStream)) {
					for (int i = 0; i < 4; i++)
						is.read();
					return mapper.readTree(is);
				}
			});

			return Read.from(json) //
					.map2(json_ -> toSymbol(json_.get("t").textValue()),
							json_ -> Float.parseFloat(json_.get("l").textValue().replace(",", ""))) //
					.toMap();

		} else
			return new HashMap<>();
	}

	private String fromSymbol(String symbol) {
		if (symbol.startsWith("^"))
			return symbol.substring(1);
		else if (symbol.endsWith(".HK"))
			return symbol.substring(0, 4);
		else
			throw new RuntimeException();
	}

	private String toSymbol(String code) {
		if (code.length() == 4)
			return code + ".HK";
		else
			return "^" + code;
	}

}
