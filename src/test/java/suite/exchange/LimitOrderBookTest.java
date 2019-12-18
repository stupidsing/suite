package suite.exchange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import primal.Verbs.Get;
import primal.os.Log_;
import suite.exchange.LimitOrderBook.LobListener;

public class LimitOrderBookTest {

	@Test
	public void test() {
		var listener = new LobListener<String>() {
			public void handleOrderFulfilled(LimitOrderBook<String>.Order order, float price, int buySell) {
				Log_.info("ORDER " + order.id + " BS " + buySell);
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
				order.isMarket = true;
				order.price = price;
				order.buySell = buySell;
				return order;
			}
		};

		var o0 = o.newOrder(1f, +100);
		var o1 = o.newOrder(1f, +100);
		var o2 = o.newOrder(1f, -200);
		lob.update(null, o0);
		lob.update(null, o1);
		lob.update(null, o2);

		assertEquals(o0.buySell, o0.xBuySell);
		assertEquals(o1.buySell, o1.xBuySell);
		assertEquals(o2.buySell, o2.xBuySell);
	}

}
