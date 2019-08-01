package suite.adt.map;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import primal.primitive.adt.pair.IntObjPair;
import suite.primitive.adt.map.IntObjMap;

public class IntObjMapTest {

	@Test
	public void test() {
		var map = new IntObjMap<Integer>();
		map.put(1, 2);
		map.put(3, 4);
		map.put(5, 6);
		assertEquals(Integer.valueOf(2), map.get(1));
		assertEquals(Integer.valueOf(4), map.get(3));
		assertEquals(Integer.valueOf(6), map.get(5));

		var expected = Set.of("1:2", "3:4", "5:6");
		var actual = new HashSet<>();

		var source = map.source();
		var pair = IntObjPair.of(0, 0);

		while (source.source2(pair))
			actual.add(pair.k + ":" + pair.v);

		assertEquals(expected, actual);
	}

}
