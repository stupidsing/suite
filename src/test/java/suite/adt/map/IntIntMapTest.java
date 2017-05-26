package suite.adt.map;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import suite.adt.pair.IntIntPair;
import suite.primitive.IntPrimitiveSource.IntIntSource;

public class IntIntMapTest {

	@Test
	public void test() {
		IntIntMap map = new IntIntMap();
		map.put(1, 2);
		map.put(3, 4);
		map.put(5, 6);
		assertEquals(2, map.get(1));
		assertEquals(4, map.get(3));
		assertEquals(6, map.get(5));

		Set<String> expected = new HashSet<>();
		expected.add("1:2");
		expected.add("3:4");
		expected.add("5:6");

		Set<String> actual = new HashSet<>();

		IntIntSource source = map.source();
		IntIntPair pair = IntIntPair.of(0, 0);
		while (source.source2(pair))
			actual.add(pair.t0 + ":" + pair.t1);
		assertEquals(expected, actual);
	}

}
