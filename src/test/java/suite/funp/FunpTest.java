package suite.funp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.assembler.Amd64.Instruction;
import suite.funp.Funp_.Funp;
import suite.funp.P1GenerateLambda.Int;
import suite.funp.P1GenerateLambda.Rt;
import suite.funp.P1GenerateLambda.Thunk;
import suite.funp.P1GenerateLambda.Value;
import suite.immutable.IMap;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class FunpTest {

	private P0Parse p0 = new P0Parse();
	private P1InferType p1 = new P1InferType();
	private P1GenerateLambda p1g = new P1GenerateLambda();
	private P2GenerateCode p2 = new P2GenerateCode();

	@Test
	public void testArray1() {
		test("define a := array (0,) >> a {1}");
	}

	@Test
	public void testArray3() {
		test("define a := array (0, 1, 2,) >> a {1}");
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
		test("1 + 2 * (3 + 4)");
	}

	@Test
	public void testInterpret() {
		assertEquals(7, interpret(Suite.parse("1 + 2 * 3")));
		assertEquals(1, interpret(Suite.parse("0 | (a => a + 1)")));
	}

	@Test
	public void testLambda() {
		test("0 | (a => a + 1)");
	}

	@Test
	public void testReference() {
		test("define i := 3 >> define p := address i >> 2 + ^p");
	}

	private void test(String p) {
		LogUtil.info(p);
		Bytes bytes = compile(p);
		LogUtil.info("Hex" + bytes + "\n\n");
		assertTrue(bytes != null);
	}

	private Bytes compile(String fp) {
		Node node = Suite.parse(fp);
		Funp f0 = p0.parse(node);
		Funp f1 = p1.infer(f0);
		List<Instruction> instructions = p2.compile0(f1);
		return p2.compile1(0, instructions, true);
	}

	private int interpret(Node node) {
		Funp f0 = p0.parse(node);
		p1.infer(f0);
		Thunk thunk = p1g.compile(0, IMap.empty(), f0);
		Value value = thunk.apply(new Rt(null, null));
		return ((Int) value).i;
	}

}
