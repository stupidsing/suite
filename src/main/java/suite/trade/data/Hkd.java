package suite.trade.data;

import static suite.util.Fail.fail;

import java.util.Map;
import java.util.Set;

import suite.math.linalg.Vector;
import suite.streamlet.Read;
import suite.trade.Instrument;
import suite.trade.TimeRange;
import suite.util.String_;

public class Hkd {

	private Vector vec = new Vector();

	public DataSource dataSource(String symbol, TimeRange period) {
		return String_.equals(symbol, Instrument.cashSymbol) //
				? DataSource.of(new long[] { period.to.epochSec(), }, vec.of(1f)) //
				: fail();
	}

	public Instrument queryCompany(String symbol) {
		if (String_.equals(symbol, Instrument.cashSymbol))
			return Instrument.cash;
		else if (String_.equals(symbol, Instrument.hsiSymbol))
			return Instrument.hsi;
		else
			return fail();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return Read //
				.from(symbols) //
				.map2(symbol -> String_.equals(symbol, Instrument.cashSymbol) ? 1f : fail()) //
				.toMap();
	}

}
