package suite.fp.intrinsic;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.util.Comparer;

public class IntrinsicTest {

	@Test
	public void testCharsReplace() {
		var fp = "cs-replace {\"abc\" | cs-from-string} {\"def\" | cs-from-string}  {\"012abcdefghi\" | cs-from-string} | cs-to-string";
		var expect = Suite.evaluateFun("\"012defdefghi\"", true);
		var actual = Suite.evaluateFun(fp, true);
		assertTrue(Comparer.comparer.compare(expect, actual) == 0);
	}

}
