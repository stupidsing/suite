package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Node;

public class OlderFunTest {

	private static String concatList0 = """
			define concat-list0 := split_{h => t =>
			    if-list_{h}_{h1 => t1 => h1; concat-list0_{t1; t}}_{concat-list0_{t}}
			} ~
			""";

	private static String filter0 = """
			define filter0 := fun =>
			    split_{h => t =>
			        define others := filter0_{fun}_{t} ~
			        if (fun_{h}) then (h; others) else others
			    }
			~
			""";

	private static String ifTree = """
			define if-list := list => f1 => f2 =>
			    if (is-list_{list}) then (
			        f1_{head_{list}}_{tail_{list}}
			    )
			    else f2
			~
			""";

	private static String map0 = "define map0 := fun => split_{h => t => fun_{h}; map0_{fun}_{t}} ~ \n";

	private static String split = "define split := fun => list => if-list_{list}_{fun}_{} ~ \n";

	@Test
	public void testConcat() {
		var fp0 = "" //
				+ ifTree + split + concatList0 //
				+ "concat-list0_{(1; 2;); (3; 4;); (5; 6;);}";
		assertEquals(Suite.parse("1; 2; 3; 4; 5; 6;"), eval(fp0));
	}

	@Test
	public void testFilter() {
		var fp1 = "" //
				+ ifTree + split + filter0 //
				+ "filter0_{n => n % 2 = 0}_{3; 4; 5; 6;}";
		assertEquals(Suite.parse("4; 6;"), eval(fp1));
	}

	@Test
	public void testMap() {
		var fp2 = "" //
				+ ifTree + split + map0 //
				+ "map0_{n => n + 2}_{3; 4; 5;}";
		assertEquals(Suite.parse("5; 6; 7;"), eval(fp2));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
