package suite.exchange;

import java.util.Map.Entry;
import java.util.TreeMap;

public class LimitOrderBook<Id> {

	private TreeMap<Float, Order> buyOrders = new TreeMap<>();
	private TreeMap<Float, Order> sellOrders = new TreeMap<>();
	private LobListener<Id> listener;

	public interface LobListener<Id> {
		public void handleOrderFulfilled(LimitOrderBook<Id>.Order order, float price, int buySell);

		public void handleOrderDisposed(LimitOrderBook<Id>.Order order);

		public void handleQuoteChanged(float bid, float ask, int volume);
	}

	public class Order {
		public Id id;
		public boolean isMarket;
		public float price;
		public int buySell; // total quantity, signed, negative for sell
		public int xBuySell; // executed quantity, signed, negative for sell
		public Order prev = this, next = this;

		private boolean isEmpty() {
			return prev == this;
		}

		private void insertNext(Order order) {
			order.prev = this;
			order.next = next;
			next.prev = order;
			next = order;
		}

		private void delete() {
			prev.next = next;
			next.prev = prev;
		}
	}

	public LimitOrderBook(LobListener<Id> listener) {
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
		var total = 0;

		while ((be = buyOrders.lastEntry()) != null && (se = sellOrders.firstEntry()) != null) {
			bp = be.getKey();
			sp = se.getKey();
			var bq = be.getValue();
			var sq = se.getValue();

			if (!bq.isEmpty() && !sq.isEmpty() && sp <= bp) {
				var bo = bq.prev;
				var so = sq.prev;
				var price = bo.isMarket ? so.price : so.isMarket ? bo.price : (bo.price + so.price) * .5f;
				var quantity = Math.min(bo.buySell - bo.xBuySell, so.xBuySell - so.buySell);

				bo.xBuySell += quantity;
				so.xBuySell -= quantity;

				listener.handleOrderFulfilled(bo, price, +quantity);
				listener.handleOrderFulfilled(so, price, -quantity);
				total += quantity;

				disposeIfCompleted(buyOrders, be, bo);
				disposeIfCompleted(sellOrders, se, so);
			} else
				break;
		}

		listener.handleQuoteChanged(bp, sp, total);
	}

	private void disposeIfCompleted(TreeMap<Float, Order> orders, Entry<Float, Order> entry, Order order) {
		if (order.buySell == order.xBuySell) {
			listener.handleOrderDisposed(order);
			order.delete();
			if (entry.getValue().isEmpty())
				orders.remove(entry.getKey());
		}
	}

}
