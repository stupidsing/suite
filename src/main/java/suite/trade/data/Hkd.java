package suite.trade.data;

import static primal.statics.Fail.fail;

import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import suite.math.linalg.Vector;
import suite.trade.Instrument;
import suite.trade.TimeRange;

public class Hkd {

	private Vector vec = new Vector();

	public DataSource dataSource(String symbol, TimeRange period) {
		return Equals.string(symbol, Instrument.cashSymbol) //
				? DataSource.of(new long[] { period.to.epochSec(), }, vec.of(1f)) //
				: fail();
	}

	public Instrument queryCompany(String symbol) {
		if (Equals.string(symbol, Instrument.cashSymbol))
			return Instrument.cash;
		else if (Equals.string(symbol, Instrument.hsiSymbol))
			return Instrument.hsi;
		else
			return fail();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return Read //
				.from(symbols) //
				.map2(symbol -> Equals.string(symbol, Instrument.cashSymbol) ? 1f : fail()) //
				.toMap();
	}

}
