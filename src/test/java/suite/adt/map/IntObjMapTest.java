package suite.adt.map;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;

public class IntObjMapTest {

	@Test
	public void test() {
		IntObjMap<Integer> map = new IntObjMap<>();
		map.put(1, 2);
		map.put(3, 4);
		map.put(5, 6);
		assertEquals(Integer.valueOf(2), map.get(1));
		assertEquals(Integer.valueOf(4), map.get(3));
		assertEquals(Integer.valueOf(6), map.get(5));

		var expected = new HashSet<>();
		expected.add("1:2");
		expected.add("3:4");
		expected.add("5:6");

		var actual = new HashSet<>();

		IntObjSource<Integer> source = map.source();
		IntObjPair<Integer> pair = IntObjPair.of(0, 0);

		while (source.source2(pair))
			actual.add(pair.t0 + ":" + pair.t1);

		assertEquals(expected, actual);
	}

}
