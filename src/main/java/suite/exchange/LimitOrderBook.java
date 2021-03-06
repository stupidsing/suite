package suite.exchange;

import static java.lang.Math.min;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class LimitOrderBook<Key> {

	private TreeMap<Float, Order> buyOrders = new TreeMap<>();
	private TreeMap<Float, Order> sellOrders = new TreeMap<>();
	private float lastPrice = Float.NaN;
	private LobListener<Key> listener;

	public interface LobListener<Key> {
		public void handleOrderFulfilled(LimitOrderBook<Key>.Order order, float price, int buySell);

		public void handleOrderDisposed(LimitOrderBook<Key>.Order order);

		public void handleQuoteChanged(float bid, float ask, int volume);
	}

	public class Order {
		public Key key;
		public float price; // NaN for market order
		public int buySell; // total quantity, signed, negative for sell
		public int xBuySell; // executed quantity, signed, negative for sell
		public long expiry = Long.MAX_VALUE;
		public Order prev = this, next = this;

		public Order() {
		}

		public Order(Key key, float price, int buySell) {
			this.key = key;
			this.price = price;
			this.buySell = buySell;
		}

		private boolean isEmpty() {
			return prev == this;
		}

		private boolean isMarket() {
			return Float.isNaN(price);
		}

		private boolean isValid(long now) {
			return now < expiry;
		}

		private float price() {
			return !isMarket() ? price : 0 < buySell ? Float.MAX_VALUE : Float.MIN_VALUE;
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

	public LimitOrderBook(LobListener<Key> listener) {
		this.listener = listener;
	}

	public synchronized void update(Order order0, Order orderx) {
		var qm0 = order0 != null ? (0 < order0.buySell ? buyOrders : sellOrders) : null;
		var qmx = orderx != null ? (0 < orderx.buySell ? buyOrders : sellOrders) : null;
		var q0 = qm0 != null ? qm0.computeIfAbsent(order0.price(), p -> new Order()) : null;
		var qx = qmx != null ? qmx.computeIfAbsent(orderx.price(), p -> new Order()) : null;

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

	public float getLastPrice() {
		return lastPrice;
	}

	private void match() {
		long now = System.currentTimeMillis();
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
				int quantity;

				if (!bo.isValid(now))
					dispose(buyOrders, be, bo);
				else if (!so.isValid(now))
					dispose(sellOrders, se, so);
				else {
					if (bo.isMarket() && so.isMarket())
						; // follows previous price
					else if (bo.isMarket())
						lastPrice = sp;
					else if (so.isMarket())
						lastPrice = bp;
					else
						lastPrice = (bp + sp) * .5f;

					quantity = min(bo.buySell - bo.xBuySell, so.xBuySell - so.buySell);

					bo.xBuySell += quantity;
					so.xBuySell -= quantity;

					listener.handleOrderFulfilled(bo, lastPrice, +quantity);
					listener.handleOrderFulfilled(so, lastPrice, -quantity);
					total += quantity;

					disposeIfCompleted(buyOrders, be, bo);
					disposeIfCompleted(sellOrders, se, so);
				}
			} else
				break;
		}

		listener.handleQuoteChanged(bp, sp, total);
	}

	private void disposeIfCompleted(Map<Float, Order> orders, Entry<Float, Order> entry, Order order) {
		if (order.buySell == order.xBuySell)
			dispose(orders, entry, order);
	}

	private void dispose(Map<Float, Order> orders, Entry<Float, Order> entry, Order order) {
		listener.handleOrderDisposed(order);
		order.delete();
		if (entry.getValue().isEmpty())
			orders.remove(entry.getKey());
	}

}
