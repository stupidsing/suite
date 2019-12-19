package suite.exchange;

import java.util.ArrayDeque;
import java.util.Deque;

import primal.MoreVerbs.Read;
import primal.primitive.fp.AsDbl;
import suite.trade.Trade;

public class ExPosition {

	private int buySell;
	private Deque<Fifo> fifos = new ArrayDeque<>();

	private class Fifo {
		private int buySell;
		private float entryPrice;
	}

	public synchronized double enqueueFifo(Trade trade) {
		var last = new Fifo(); // outstanding
		last.buySell = trade.buySell;
		last.entryPrice = trade.price;

		var realizedPnl = 0f;
		Fifo first;

		while ((first = fifos.peekFirst()) != null && Math.signum(last.buySell) != Math.signum(first.buySell)) {
			int bsd;
			if (last.buySell < 0)
				bsd = Math.min(-last.buySell, first.buySell);
			else
				bsd = Math.max(-last.buySell, first.buySell);

			last.buySell += bsd;
			first.buySell -= bsd;
			realizedPnl += bsd * (last.entryPrice - first.entryPrice);

			if (first.buySell == 0)
				fifos.removeFirst();
		}

		if (last.buySell != 0)
			fifos.addLast(last);

		return realizedPnl;
	}

	public void adjust(int bsd) {
		buySell += bsd;
	}

	public int getBuySell() {
		return buySell;
	}

	public synchronized ExSummary summary(float currentPrice, double invLeverage) {
		var n = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell * fifo.entryPrice));
		var d = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell));

		var summary = new ExSummary();
		summary.vwapEntryPrice = n / d;
		summary.unrealizedPnl = Read.from(fifos)
				.toDouble(AsDbl.sum(fifo -> fifo.buySell * (currentPrice - fifo.entryPrice)));
		summary.investedAmount = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell * currentPrice));
		summary.marginUsed = summary.investedAmount * invLeverage;
		return summary;
	}

}
