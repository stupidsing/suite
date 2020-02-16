package suite.os;

import org.junit.jupiter.api.Test;

import suite.cfg.Defaults;
import suite.node.util.Singleton;
import suite.streamlet.As;

public class StoreCacheTest {

	@Test
	public void test() {
		var size = Defaults //
				.bindSecrets("stockUrl .0") //
				.map(Singleton.me.storeCache::http) //
				.collect(As::table) //
				.size();

		System.out.println("size = " + size);
	}

}
