package suite.exchange;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import primal.Verbs.Get;
import primal.os.Log_;
import suite.exchange.LimitOrderBook.LobListener;

public class LimitOrderBookTest {

	@Test
	public void test() {
		var listener = new LobListener<String>() {
			public void handleOrderFulfilled(LimitOrderBook<String>.Order order, float price, int buySell) {
				Log_.info("ORDER " + order.id + " P " + price + " Q " + buySell);
			}

			public void handleOrderDisposed(LimitOrderBook<String>.Order order) {
				Log_.info("ORDER " + order.id + " ENDED");
			}

			public void handleQuoteChanged(float bid, float ask, int volume) {
			}
		};

		var lob = new LimitOrderBook<String>(listener);

		var o = new Object() {
			private LimitOrderBook<String>.Order newOrder(float price, int buySell) {
				var order = lob.new Order();
				order.id = "O" + Get.temp();
				order.price = price;
				order.buySell = buySell;
				return order;
			}
		};

		var o0 = o.newOrder(1.01f, +100);
		var o1 = o.newOrder(1f, +100);
		var o2 = o.newOrder(Float.NaN, -50);
		var o3 = o.newOrder(1f, -150);
		var orders = List.of(o0, o1, o2, o3);

		for (var order : orders)
			lob.update(null, order);

		for (var order : orders)
			assertEquals(order.buySell, order.xBuySell);
	}

}
