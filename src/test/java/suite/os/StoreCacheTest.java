package suite.os;

import org.junit.Test;

import suite.Defaults;
import suite.node.util.Singleton;
import suite.streamlet.As;

public class StoreCacheTest {

	@Test
	public void test() {
		var url = Defaults.secrets("stockUrl .0")[0];

		var size = Singleton.me.storeCache //
				.http(url) //
				.collect(As::table) //
				.size();

		System.out.println("size = " + size);
	}

}
