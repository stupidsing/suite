package suite.funp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.funp.Funp_.Main;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class FunpTest {

	@Test
	public void testArray1() {
		test("define a := array (0,) >> a {1}");
	}

	@Test
	public void testArray3() {
		test("define a := array (0, 1, 2,) >> a {1}");
	}

	@Test
	public void testBind() {
		test("define a := array (0, 1,) >> if (`array (0, v,)` := a) then v else 0");
	}

	@Test
	public void testCompare() {
		test("define v := 2 >> if (1 < v) then 1 else 0");
	}

	@Test
	public void testDefine() {
		test("define i := 3 >> i + 1");
		test("define f := i => i + 1 >> 3 | f");
	}

	@Test
	public void testExpr0() {
		test("1 + 2 * 3");
	}

	@Test
	public void testExpr1() {
		test("1 + 2 * (3 + 4) / 7");
	}

	@Test
	public void testInterpret() {
		Main main = Funp_.main();
		assertEquals(7, main.interpret(Suite.parse("1 + 2 * 3")));
		assertEquals(1, main.interpret(Suite.parse("0 | (a => a + 1)")));
	}

	@Test
	public void testLambda() {
		test("0 | (a => a + 1)");
	}

	@Test
	public void testReference() {
		test("define i := 3 >> define p := address i >> 2 + ^p");
	}

	@Test
	public void testStruct() {
		test("define s := struct (a 1, b 2, c 3,) >> s/c");
	}

	private void test(String p) {
		LogUtil.info(p);
		Bytes bytes = Funp_.main().compile(0, p);
		LogUtil.info("Hex" + bytes + "\n\n");
		assertTrue(bytes != null);
	}

}
