package suite.funp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64Interpret;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class FunpTest {

	@Test
	public void testArray1() {
		test(0, "define a := array (0,) >> a {0}");
	}

	@Test
	public void testArray3() {
		test(1, "define a := array (0, 1, 2,) >> a {1}");
	}

	@Test
	public void testBind() {
		test(1, "define a := array (0, 1,) >> if (`array (0, v,)` = a) then v else 0");
	}

	@Test
	public void testCapture() {
		test(31, "define m := 31 >> 15 | (n => m)");
		test(0, "define m := pointer => 0 >> 1 | (n => 2 | m)");

		test(0, "" //
				+ "define f := j => 0 >> " //
				+ "define g := j => 0 >> " //
				+ "define h := j => 0 >> " //
				+ "0 | (i => 0 | f | g | h)");
	}

	@Test
	public void testCompare() {
		test(1, "define v := 2 >> if (1 < v) then 1 else 0");
	}

	@Test
	public void testDefine() {
		test(4, "define i := 3 >> i + 1");
		test(4, "define f := i => i + 1 >> 3 | f");
	}

	@Test
	public void testExpr0() {
		test(7, "1 + 2 * 3");
	}

	@Test
	public void testExpr1() {
		test(3, "1 + 2 * (3 + 4) / 7");
	}

	@Test
	public void testIo() {
		test(1, "io 0 | io-cat (i => io (i + 1))");
	}

	@Test
	public void testIterate() {
		test(100, "iterate v 0 (v < 100) (io (v + 1))");
	}

	@Test
	public void testGlobal() {
		test(1, "global a := array (0, 1, 2,) >> a {1}");
	}

	@Test
	public void testLambda() {
		test(1, "0 | (a => a + 1)");
	}

	@Test
	public void testLambdaReturn() {
		test(2, "1 | (0 | (a => b => b + 1))");
	}

	@Test
	public void testReference() {
		test(5, "define i := 3 >> define p := address i >> 2 + ^p");
	}

	@Test
	public void testSeq() {
		test(3, "0; 1; 2; 3");
	}

	@Test
	public void testStruct() {
		test(3, "define s := struct (a 1, b 2, c 3,) >> s/c");
	}

	private void test(int r, String p) {
		LogUtil.info(p);
		Pair<List<Instruction>, Bytes> pair = Funp_.main().compile(0, p);
		var bytes = pair.t1;
		LogUtil.info("Hex" + bytes + "\n\n");
		assertEquals(r, new Amd64Interpret().interpret(pair.t0, Bytes.of(), Bytes.of()));
		assertTrue(bytes != null);
	}

}
