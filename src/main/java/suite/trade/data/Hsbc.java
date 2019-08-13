package suite.trade.data;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.streamlet.Streamlet;
import suite.os.Execute;
import suite.streamlet.As;
import suite.util.XmlUtil;

public class Hsbc {

	private XmlUtil xmlUtil = new XmlUtil();

	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	public synchronized Map<String, Float> quote(Set<String> symbols) {
		return quoteCache.quote(symbols, "-");
	}

	private Map<String, Float> quote_(Streamlet<String> symbols, String dummy) {
		return Read //
				.from(symbols) //
				.map2(symbol -> {
					var command = "curl -H 'User-Agent: Mozilla/5.0' 'https://www.personal.hsbc.com.hk/1/PA_HBAPPWSwebsource/pwsfile" //
							+ "?resource=mistockquotetext" //
							+ "&stockCode=" + toHsbc(symbol) //
							+ "&stockName=" //
							+ "&lang=en'";

					var is = new ByteArrayInputStream(Execute.shell(command).getBytes(Utf8.charset));
					var xml = ex(() -> xmlUtil.read(is));

					var quote = Read //
							.each(xml) //
							.concatMap(xml_ -> xml_.children("response")) //
							.concatMap(xml_ -> xml_.children("quote")) //
							.concatMap(xml_ -> xml_.children("quoteResult")) //
							.concatMap(xml_ -> xml_.children("nominal")) //
							.uniqueResult( //
					).text();

					return Float.valueOf(quote);
				}) //
				.collect(As::map);
	}

	private String toHsbc(String symbol_) {
		if (symbol_.endsWith(".HK"))
			return "0" + symbol_.substring(0, symbol_.length() - 3);
		else
			return fail();
	}

}
