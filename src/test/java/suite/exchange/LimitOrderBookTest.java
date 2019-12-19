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
				Log_.info("ORDER " + order.key + " P " + price + " Q " + buySell);
			}

			public void handleOrderDisposed(LimitOrderBook<String>.Order order) {
				Log_.info("ORDER " + order.key + " ENDED");
			}

			public void handleQuoteChanged(float bid, float ask, int volume) {
			}
		};

		var lob = new LimitOrderBook<String>(listener);

		var o = new Object() {
			private LimitOrderBook<String>.Order newOrder(float price, int buySell) {
				var order = lob.new Order();
				order.key = "O" + Get.temp();
				order.price = price;
				order.buySell = buySell;
				return order;
			}
		};

		var orders = List.of( //
				o.newOrder(1.01f, +100), //
				o.newOrder(1f, +100), //
				o.newOrder(Float.NaN, -50), //
				o.newOrder(1f, -150));

		for (var order : orders)
			lob.update(null, order);

		for (var order : orders)
			assertEquals(order.buySell, order.xBuySell);
	}

}
