package suite.os;

import org.junit.Test;

import suite.node.util.Singleton;
import suite.streamlet.As;

public class StoreCacheTest {

	@Test
	public void test() {
		int size = Singleton.me.getStoreCache() //
				.http("https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt") //
				.collect(As::table) //
				.size();
		System.out.println("size = " + size);
	}

}
