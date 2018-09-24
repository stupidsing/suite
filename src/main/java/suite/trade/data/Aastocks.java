package suite.trade.data;

import suite.http.HttpUtil;
import suite.primitive.Chars_;
import suite.primitive.Ints_;
import suite.primitive.adt.map.ObjFltMap;
import suite.trade.Trade_;

public class Aastocks {

	private ObjFltMap<String> quoteCache = new ObjFltMap<>();

	public float hsi() {
		var url = "http://www.aastocks.com/en/mobile/Quote.aspx?symbol=00005";
		var lines = HttpUtil.get(url).lines().toList();
		var i0 = Ints_.for_(lines.size()).filter(i -> lines.get(i).contains("HSI")).first();
		return toFloat(lines.get(i0 + 1));
	}

	public float quote(String symbol) {
		if (Trade_.isCacheQuotes)
			return quoteCache.computeIfAbsent(symbol, this::quote_);
		else
			return quote_(symbol);
	}

	private float quote_(String symbol) {
		var url = "http://www.aastocks.com/en/mobile/Quote.aspx?symbol=0" + symbol.substring(0, 4);
		var lines = HttpUtil.get(url).lines().toList();
		var i0 = Ints_.for_(lines.size()).filter(i -> lines.get(i).contains("text_last")).first();
		return toFloat(lines.get(i0 + 1).replace("0px", "").replace(".png", ""));
	}

	private float toFloat(String s) {

		// remove all quoted strings
		while (true) {
			var p0 = s.indexOf('"');
			var p1 = 0 <= p0 ? s.indexOf('"', p0 + 1) : -1;
			if (0 <= p1)
				s = s.substring(0, p0 - 1) + s.substring(p1 + 1);
			else
				break;
		}

		// extract the number we want
		return Float.parseFloat(new String(Chars_.of(s.toCharArray()) //
				.filter(c -> c == '.' || '0' <= c && c <= '9') //
				.toArray()));
	}

}
