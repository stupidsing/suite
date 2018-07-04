package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class OlderFunTest {

	private static String concatList0 = "" //
			+ "define concat-list0 := split {h => t => \n" //
			+ "    if-list {h} {h1 => t1 => h1; concat-list0 {t1; t}} {concat-list0 {t}} \n" //
			+ "} ~ \n";

	private static String filter0 = "" //
			+ "define filter0 := fun => \n" //
			+ "    split {h => t => \n" //
			+ "        define others := filter0 {fun} {t} ~ \n" //
			+ "        if (fun {h}) then (h; others) else others \n" //
			+ "    } \n" //
			+ "~ \n";

	private static String ifTree = "" //
			+ "define if-list := list => f1 => f2 => \n" //
			+ "    if (is-list {list}) then ( \n" //
			+ "        f1 {head {list}} {tail {list}} \n" //
			+ "    ) \n" //
			+ "    else f2 \n" //
			+ "~ \n";

	private static String map0 = "define map0 := fun => split {h => t => fun {h}; map0 {fun} {t}} ~ \n";

	private static String split = "define split := fun => list => if-list {list} {fun} {} ~ \n";

	@Test
	public void testConcat() {
		var fp0 = "" //
				+ ifTree + split + concatList0 //
				+ "concat-list0 {(1; 2;); (3; 4;); (5; 6;);}";
		assertEquals(Suite.parse("1; 2; 3; 4; 5; 6;"), eval(fp0));
	}

	@Test
	public void testFilter() {
		var fp1 = "" //
				+ ifTree + split + filter0 //
				+ "filter0 {n => n % 2 = 0} {3; 4; 5; 6;}";
		assertEquals(Suite.parse("4; 6;"), eval(fp1));
	}

	@Test
	public void testMap() {
		var fp2 = "" //
				+ ifTree + split + map0 //
				+ "map0 {n => n + 2} {3; 4; 5;}";
		assertEquals(Suite.parse("5; 6; 7;"), eval(fp2));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
