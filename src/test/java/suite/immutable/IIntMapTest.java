package suite.immutable;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IIntMapTest {

	@Test
	public void test() {
		var map = new IIntMap<>();

		for (var i = 0; i < 256; i++) {
			var i_ = i;
			map = map.update(i, v -> i_);
		}

		for (var i = 0; i < 256; i++)
			assertEquals(i, (int) map.get(i));
	}

}
