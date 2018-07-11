package suite.trade.data; import static suite.util.Friends.fail;

import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.util.String_;

public class Hkd {

	public DataSource dataSource(String symbol, TimeRange period) {
		return String_.equals(symbol, Asset.cashSymbol) //
				? DataSource.of(new long[] { period.to.epochSec(), }, new float[] { 1f, }) //
				: fail();
	}

	public Asset queryCompany(String symbol) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return Asset.cash;
		else if (String_.equals(symbol, Asset.hsiSymbol))
			return Asset.hsi;
		else
			return fail();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return Read //
				.from(symbols) //
				.map2(symbol -> String_.equals(symbol, Asset.cashSymbol) ? 1f : fail()) //
				.toMap();
	}

}
