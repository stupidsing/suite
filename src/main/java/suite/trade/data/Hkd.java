package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.util.Fail;
import suite.util.String_;

public class Hkd {

	public DataSource dataSource(String symbol, TimeRange period) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return DataSource.of(new long[] { period.to.epochSec(), }, new float[] { 1f, });
		else
			return Fail.t();
	}

	public Asset queryCompany(String symbol) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return Asset.cash;
		else if (String_.equals(symbol, Asset.hsiSymbol))
			return Asset.hsi;
		else
			return Fail.t();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return Read.from(symbols) //
				.map2(symbol -> {
					if (String_.equals(symbol, Asset.cashSymbol))
						return 1f;
					else
						return Fail.t();
				}) //
				.toMap();
	}

}
