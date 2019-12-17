package suite.exchange;

import java.util.Map.Entry;
import java.util.TreeMap;

public class LimitOrderBook {

	private TreeMap<Float, Order> buyOrders = new TreeMap<>();
	private TreeMap<Float, Order> sellOrders = new TreeMap<>();
	private LobListener listener;

	public interface LobListener {
		public void handleOrderFulfilled(Order order, float price, int quantity);

		public void handleOrderDisposed(Order order);

		public void handleQuoteChange(float bid, float ask);
	}

	public class Order {
		public String id;
		public boolean isMarket;
		public float price;
		public int buySell; // signed quantity, negative for sell
		public int executedBuySell;
		public Order prev, next;

		public Order() {
			prev = this;
			next = this;
		}

		private boolean isEmpty() {
			return prev == next;
		}

		private void insertNext(Order order) {
			order.prev = this;
			order.next = next;
			next.prev = order;
			next = order;
		}

		private void delete() {
			prev.next = next.prev;
			next.prev = prev.next;
		}
	}

	public LimitOrderBook(LobListener listener) {
		this.listener = listener;
	}

	public synchronized void update(Order order0, Order orderx) {
		var qm0 = order0 != null ? (0 < order0.buySell ? buyOrders : sellOrders) : null;
		var qmx = orderx != null ? (0 < orderx.buySell ? buyOrders : sellOrders) : null;
		var q0 = qm0 != null ? qm0.computeIfAbsent(order0.price, p -> new Order()) : null;
		var qx = qmx != null ? qmx.computeIfAbsent(orderx.price, p -> new Order()) : null;

		if (q0 == qx) {
			orderx.price = order0.price;
			orderx.buySell = order0.buySell;
		} else { // requeue
			if (order0 != null) {
				order0.delete();
				if (q0.isEmpty())
					qm0.remove(order0.price);
			}
			if (orderx != null)
				qx.insertNext(orderx);
		}

		match();
	}

	private void match() {
		Entry<Float, Order> be, se;
		var bp = Float.NaN;
		var sp = Float.NaN;

		while ((be = buyOrders.lastEntry()) != null && (se = sellOrders.firstEntry()) != null) {
			bp = be.getKey();
			sp = se.getKey();
			var bq = be.getValue();
			var sq = se.getValue();

			if (!bq.isEmpty() && !sq.isEmpty() && sp <= bp) {
				var bo = bq.prev;
				var so = sq.prev;
				var price = bo.isMarket ? so.price : so.isMarket ? bo.price : (bo.price + so.price) / 2f;
				var quantity = Math.min(bo.buySell - bo.executedBuySell, so.executedBuySell - so.buySell);

				bo.executedBuySell += quantity;
				so.executedBuySell -= quantity;

				listener.handleOrderFulfilled(bo, price, +quantity);
				listener.handleOrderFulfilled(so, price, -quantity);

				if (bo.buySell == bo.executedBuySell) {
					listener.handleOrderDisposed(bo);
					bo.delete();
					if (bq.isEmpty())
						buyOrders.remove(bp);
				}

				if (so.buySell == so.executedBuySell) {
					listener.handleOrderDisposed(so);
					so.delete();
					if (sq.isEmpty())
						sellOrders.remove(sp);
				}
			} else
				break;
		}

		listener.handleQuoteChange(bp, sp);
	}

}
