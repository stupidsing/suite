package suite.fp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.io.Formatter;
import suite.streamlet.ReadChars;

public class Fun23TreeTest {

	@Test
	public void test() throws IOException {
		var n = 100;
		var list100 = "0 until " + n + " | map_{insert} | apply | {Tree (9999, Empty;)}";

		var fp0 = Suite.substitute("use 23-TREE ~ " + list100);
		var result0 = Suite.evaluateFun(Suite.fcc(fp0, false));
		var out0 = Formatter.dump(result0);
		System.out.println("OUT:\n" + out0);

		var nPars0 = ReadChars.from(out0).filter(c -> c == '(').size();
		assertTrue(3 <= nPars0);

		var fp1 = Suite.substitute("use 23-TREE ~ 0 until " + n / 2 + " | map_{remove} | apply | {" + list100 + "}");
		var result1 = Suite.evaluateFun(Suite.fcc(fp1, false));
		var out1 = Formatter.dump(result1);
		System.out.println("OUT:\n" + out1);

		var nPars1 = ReadChars.from(out1).filter(c -> c == '(').size();
		assertTrue(3 <= nPars1);
	}

}
