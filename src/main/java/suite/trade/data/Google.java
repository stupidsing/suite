package suite.trade.data;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.http.HttpUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class Google {

	private static ObjectMapper mapper = new ObjectMapper();

	// http://www.jarloo.com/real-time-google-stock-api/
	public synchronized Map<String, Float> quote(Streamlet<String> symbols) {
		if (0 < symbols.size()) {
			URL url = To.url("http://finance.google.com/finance/info?client=ig&q=HKEX%3A" //
					+ symbols.sort(Object_::compare).map(symbol -> symbol.substring(0, 4)).collect(As.joined(",")));

			JsonNode json = Rethrow.ex(() -> {
				try (InputStream is = HttpUtil.get(url).out.collect(To::inputStream)) {
					for (int i = 0; i < 4; i++)
						is.read();
					return mapper.readTree(is);
				}
			});

			return Read.from(json) //
					.map2(json_ -> json_.get("t").textValue() + ".HK", json_ -> Float.parseFloat(json_.get("l").textValue())) //
					.toMap();

		} else
			return new HashMap<>();
	}

}
