package suite.immutable;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IIntMapTest {

	@Test
	public void test() {
		IIntMap<Integer> map = new IIntMap<>();

		for (int i = 0; i < 256; i++) {
			int i_ = i;
			map = map.update(i, v -> i_);
		}

		for (int i = 0; i < 256; i++)
			assertEquals(i, (int) map.get(i));
	}

}
