package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class IskCombinatorTest {

	private String isk = "" //
			+ "define i := x => x >> " //
			+ "define k := x => y => x >> " //
			+ "define s := x => y => z => x {z} {y {z}} >> ";

	@Test
	public void testSksk() {
		var sksk = "s {k} {s} {k}";
		assertEquals(Suite.parse("1"), eval(isk //
				+ "(" + sksk + ") {1} {2}"));
	}

	@Test
	public void testTf() {
		var tf = "" //
				+ "define t := k >> " //
				+ "define f := k {i} >> " //
				+ "define not_ := f {t} >> " //
				+ "define or_ := k >> " //
				+ "define and_ := f >> ";

		assertEquals(Suite.parse("1"), eval(isk + tf + "t {1} {2}"));
		assertEquals(Suite.parse("2"), eval(isk + tf + "f {1} {2}"));

		// eval(isk + tf + "t {or_} {f}") becomes t
		// eval(isk + tf + "t {or_} {f}") becomes f
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
