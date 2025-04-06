package suite.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

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

		for (var i = 0; i < 256; i++)
			map = map.update(i, v -> null);

		for (var i = 0; i < 256; i++)
			assertNull(map.get(i));
	}

}
