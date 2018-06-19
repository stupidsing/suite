package suite.funp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class FunpTest {

	@Test
	public void testArray() {
		test(0, "define a := [0,] >> a:0");
		test(1, "define a := [0, 1, 2,] >> a:1");
	}

	@Test
	public void testBind() {
		test(1, "define a := [0, 1,] >> if (`[0, v,]` = a) then v else 0");
		test(0, "define a := [0, 1,] >> if (`[1, v,]` = a) then v else 0");
		test(2, "define s := { a: 1, b: 2, c: 3, } >> if (`{ a: a, b: v, c: c, }` = s) then v else 0");
		test(2, "define s := { a: 1, b: 2, c: 3, } >> if (`address ({ a: a, b: v, c: c, })` = address s) then v else 0");
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
	public void testCoerce() {
		test(1, "define i := 1 >> define b := coerce-byte i >> i");
	}

	@Test
	public void testCompare() {
		test(1, "let v := 2 >> if (1 < v) then 1 else 0");
	}

	@Test
	public void testDefine() {
		test(4, "define i := 3 >> i + 1");
		test(4, "define f := i => i + 1 >> 3 | f");
		test(1, "define global a := [0, 1, 2,] >> a:1");
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
		test(100, "io-fold 0 (n => n < 100) (n => io (n + 1))");
	}

	@Test
	public void testLambda() {
		test(1, "0 | (a => a + 1)");
		test(3, "1, 2 | ((a, b) => a + b)");
	}

	@Test
	public void testLambdaReturn() {
		test(2, "1 | (0 | (a => b => b + 1))");
	}

	@Test
	public void testRecurse() {
		test(1, "recurse (dec := n => if (0 < n) then (n - 1 | dec) else 1 #) >> 9999 | dec");
		test(89, "recurse (fib := n => if (1 < n) then ((n - 1 | fib) + (n - 2 | fib)) else 1 #) >> 10 | fib");
	}

	@Test
	public void testReference() {
		test(5, "define i := 3 >> define p := address i >> 2 + ^p");
	}

	@Test
	public void testReturnArray() {
		test(2, "define f := i => [0, 1, i,] >> (predef (2 | f)):2");
	}

	@Test
	public void testSeq() {
		test(3, "0; 1; 2; 3");
	}

	@Test
	public void testStruct() {
		test(3, "define s := { a: 1, b: 2, c: 3, } >> s/c");
	}

	private void test(int r, String p) {
		for (var isOptimize : new boolean[] { false, true, }) {
			LogUtil.info(p);
			var pair = Funp_.main(isOptimize).compile(0, p);
			var bytes = pair.t1;
			LogUtil.info("Hex" + bytes + "\n\n");
			assertEquals(r, new Amd64Interpret().interpret(pair.t0, Bytes.of(), Bytes.of()));
			assertTrue(bytes != null);
		}
	}

}
