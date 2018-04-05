package suite.adt.map;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import suite.primitive.adt.map.IntIntMap;
import suite.primitive.adt.pair.IntIntPair;

public class IntIntMapTest {

	@Test
	public void test() {
		var map = new IntIntMap();
		map.put(1, 2);
		map.put(3, 4);
		map.put(5, 6);
		assertEquals(2, map.get(1));
		assertEquals(4, map.get(3));
		assertEquals(6, map.get(5));

		var expected = new HashSet<>();
		expected.add("1:2");
		expected.add("3:4");
		expected.add("5:6");

		var actual = new HashSet<>();

		var source = map.source();
		IntIntPair pair = IntIntPair.of(0, 0);
		while (source.source2(pair))
			actual.add(pair.t0 + ":" + pair.t1);
		assertEquals(expected, actual);
	}

}
