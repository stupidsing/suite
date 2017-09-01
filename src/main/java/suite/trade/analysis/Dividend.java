package suite.trade.analysis;

import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.adt.pair.LngFltPair;
import suite.primitive.adt.pair.LngIntPair;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.Trade;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Dividend {

	public float dividend(Streamlet<Trade> trades, Fun<String, LngFltPair[]> fun) {
		float sum = 0f;

		for (Pair<String, List<Trade>> pair : trades.toMultimap(trade -> trade.symbol).listEntries()) {
			LngFltPair[] dividends = fun.apply(pair.t0);
			Outlet<Trade> outlet = Outlet.of(pair.t1);
			LngIntPair tn = LngIntPair.of(0l, 0);

			Source<LngIntPair> tradeSource = () -> {
				Trade trade = outlet.next();
				long t = trade != null ? Time.of(trade.date + " 12:00:00").epochSec(8) : Long.MAX_VALUE;
				return LngIntPair.of(t, tn.t1 + (trade != null ? trade.buySell : 0));
			};

			LngIntPair tn1 = tradeSource.source();

			for (LngFltPair dividend : dividends) {
				while (tn1 != null && tn1.t0 < dividend.t0) {
					tn.update(tn1.t0, tn1.t1);
					tn1 = tradeSource.source();
				}

				sum += tn.t1 * dividend.t1;
			}
		}

		return sum;
	}

}
