package suite.funp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.os.Log_;
import suite.primitive.Bytes;

public class FunpTest {

	private Amd64Interpret interpret = new Amd64Interpret();

	@Test
	public void testArray() {
		test(0, "define a := [0,] ~ a [0]");
		test(1, "define a := [0, 1, 2,] ~ a [1]");
	}

	@Test
	public void testBind() {
		test(1, "define a := [0, 1,] ~ if (`[0, v,]` = a) then v else 0");
		test(0, "define a := [0, 1,] ~ if (`[1, v,]` = a) then v else 0");
		test(2, "define s := { a: 1, b: 2, c: 3, } ~ if (`{ a, b: v, c, }` = s) then v else 0");
		test(2, "define s := { a: 1, b: 2, c: 3, } ~ if (`{ c, a, b: v, }` = s) then v else 0");
		test(2, "define s := { a: 1, b: 2, c: 3, } ~ if (`address.of { a, b: v, c, }` = address.of s) then v else 0");
	}

	@Test
	public void testCapture() {
		test(31, "define m := 31 ~ 15 | capture (n => m)");
		test(0, "define m j := (type j = number ~ 0) ~ 1 | capture (n => m 2)");

		test(0, "" //
				+ "define f j := (type j = number ~ 0) ~ " //
				+ "define g j := (type j = number ~ 0) ~ " //
				+ "define h j := (type j = number ~ 0) ~ " //
				+ "0 | (i => 0 | f | g | h)");
	}

	@Test
	public void testCoerce() {
		test(1, "define i := 1 ~ define b := to.byte from.number i ~ i");
	}

	@Test
	public void testCompare() {
		test(1, "let v := 2 ~ if (1 < v) then 1 else 0");
		test(1, "if ([0, 1, 2,] = [0, 1, 2,]) then 1 else 0");
		test(0, "if ([0, 1, 2,] = [0, 3, 2,]) then 1 else 0");
		test(0, "define s := { a: 1, b: 2, c: 3, } ~ if (s = { a: 1, b: 9, c: 3, }) then 1 else 0");
		test(1, "define s := { a: 1, b: 2, c: 3, } ~ if (s = { a: 1, b: 2, c: 3, }) then 1 else 0");
	}

	@Test
	public void testDefine() {
		test(4, "define i := 3 ~ i + 1");
		test(4, "define f i := i + 1 ~ 3 | f");
		test(1, "let.global a := [0, 1, 2,] ~ a [1]");
		test(1, "define { a: 1, b: ({} => me/a), c: 3, } ~ b {}");
	}

	@Test
	public void testDivMod() {
		test(4, "define { n: 43, d: 10, } ~ n / d");
		test(6, "define { n: 46, d: 10, } ~ n % d");
	}

	@Test
	public void testExpr() {
		test(7, "1 + 2 * 3");
		test(3, "1 + 2 * (3 + 4) / 7");
	}

	@Test
	public void testFold() {
		test(100, "fold (n = 0; n < 100; n + 1)");
	}

	@Test
	public void testGlobal() {
		test(0, "" //
				+ "let module := \n" //
				+ "	let.global f {} := 0 ~ \n" // global required
				+ "	let g {} := f {} ~ \n" //
				+ "	{ g, } \n" //
				+ "~ \n" //
				+ "{} | module/g");
	}

	@Test
	public void testIo() {
		test(1, "!do 1");
	}

	@Test
	public void testLambda() {
		test(2, "{} | ({} => 2)");
		test(1, "0 | (a => a + 1)");
		test(3, "1, 2 | ((a, b) => a + b)");
		test(2, "let f := { a, b: v, c, } => v ~ f { a: 1, b: 2, c: 3, }");
		test(2, "let f := [a, b, c,] => b ~ f [1, 2, 3,]");
	}

	@Test
	public void testLambdaReturn() {
		test(2, "1 | (0 | (a => b => b + 1))");
	}

	@Test
	public void testRecurse() {
		test(1, "define { dec n := if (0 < n) then (dec (n - 1)) else 1 ~ } ~ 9999 | dec");
		test(89, "define { fib n := if (1 < n) then (fib (n - 1) + fib (n - 2)) else 1 ~ } ~ 10 | fib");
	}

	@Test
	public void testReference() {
		test(5, "define i := 3 ~ define p := address.of i ~ 2 + ^p");
	}

	@Test
	public void testReturnArray() {
		test(2, "define f i := [0, 1, i,] ~ (predef (f 2)) [2]");
	}

	@Test
	public void testSeq() {
		test(3, "0; 1; 2; 3");
	}

	@Test
	public void testString() {
		test(65, "define s := \"A world for us\" ~ to.number from.byte (^s) [0]");
		test(65, "let.global s := \"A world for us\" ~ to.number from.byte (^s) [0]");
	}

	@Test
	public void testStruct() {
		test(3, "define s := { a: 1, b: 2, c: 3, } ~ s/c");
	}

	@Test
	public void testTag() {
		test(3, "if (`t:v` = t:3) then v else 0");
		test(3, "define d := t:3 ~ if (`t:v` = d) then v else 0");
		test(0, "let d := s:{} ~ type d = t:3 ~ if (`t:v` = d) then v else 0");
	}

	private void test(int expected, String program) {
		for (var isOptimize : new boolean[] { false, true, }) {
			Log_.info(program);

			var actual = Funp_ //
					.main(isOptimize) //
					.compile(interpret.codeStart, program) //
					.map((instructions, code) -> {
						Log_.info("Hex" + code + "\n\n");
						return interpret.interpret(instructions, code, Bytes.of());
					}) //
					.intValue();

			assertEquals(expected, actual);
		}
	}

}
