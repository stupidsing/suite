package suite.persistent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PerIntMapTest {

	@Test
	public void test() {
		var map = new PerIntMap<>();

		for (var i = 0; i < 256; i++) {
			var i_ = i;
			map = map.update(i, v -> i_);
		}

		for (var i = 0; i < 256; i++)
			assertEquals(i, (int) map.get(i));
	}

}
