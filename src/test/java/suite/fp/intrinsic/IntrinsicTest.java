package suite.fp.intrinsic;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.node.util.Comparer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntrinsicTest {

	@Test
	public void testCharsReplace() {
		var fp = "cs-replace_{\"abc\" | cs-from-string}_{\"def\" | cs-from-string} _{\"012abcdefghi\" | cs-from-string} | cs-to-string";
		var expect = Suite.evaluateFun("\"012defdefghi\"", true);
		var actual = Suite.evaluateFun(fp, true);
		assertTrue(Comparer.comparer.compare(expect, actual) == 0);
	}

}
