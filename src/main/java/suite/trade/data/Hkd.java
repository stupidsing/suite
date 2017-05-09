package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.util.Util;

public class Hkd {

	public Map<String, Float> quote(Set<String> stockCodes) {
		return Read.from(stockCodes) //
				.map2(stockCode -> {
					if (Util.stringEquals(stockCode, Asset.cashCode))
						return 1f;
					else
						throw new RuntimeException();
				}) //
				.toMap();
	}

}
