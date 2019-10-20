package suite.funp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import primal.os.Log_;
import primal.primitive.adt.Bytes;
import suite.assembler.Amd64Interpret;

public class FunpTest {

	private Amd64Interpret interpret = new Amd64Interpret();

	@Test
	public void testArray() {
		test(48, "define a := [number '0',] ~ a [0]");
		test(1, "let i := 1 ~ define a := [0, 1, 2,] ~ a [i]");
	}

	@Test
	public void testAsm() {
		test(3, "do! !asm (ESI = 2;) { INC ESI; }/ESI");
	}

	@Test
	public void testAssign() {
		test(3, "define p := address.of predef { a: 1, b: 2, c: 3, } ~ do! (!assign p*/b := 4 ~ p*/c)");
		test(4, "define p := address.of predef { a: 1, b: 2, c: 3, } ~ do! (!assign p*/c := 4 ~ p*/c)");
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
		test(46, "define m := 31 ~ let l := capture1 (n => n + m) ~ 15 | l");

		// unreliable when optimized. the optimizer would substitute the variable
		// definition that makes the capture1 time latter than the assignment to m.
		if (Boolean.FALSE)
			test(31, "" //
					+ "do! (" //
					+ "let m := 31 ~ " //
					+ "let l := capture1 (n => m) ~ " //
					+ "!assign m := 63 ~ " //
					+ "15 | l)");

		test(0, "define m j := (type j = number ~ 0) ~ 1 | capture1 (n => m 2)");

		// use capture1 to return a lambda expression
		test(12, "" //
				+ "define f j := capture1 (i => i + j) ~ " //
				+ "define g j := capture1 (i => i + j) ~ " //
				+ "define h j := capture1 (i => i + j) ~ " //
				+ "0 | (i => 0 | f 1 | g 2 | h 3 | f 1 | g 2 | h 3)");

		// capture1 once and calling twice! the capture1 would be freed after the first
		// call. the second call should cause problem...
		if (Boolean.FALSE)
			test(6, "" //
					+ "define f j := capture1 (i => i + j) ~ " //
					+ "define fs := f 2 ~ " //
					+ "define a := 0 | fs | fs ~ " //
					+ "define g j := capture1 (i => i + j) ~ " //
					+ "define gs := g 3 ~ " //
					+ "define b := 0 | gs | gs ~ " //
					+ "b");
	}

	@Test
	public void testCase() {
		test(5, "define i := 4 ~ case || (i = 0) => 1 || (i = 2) => 3 || (i = 4) => 5 || 6");
	}

	@Test
	public void testCoerce() {
		test(1, "define i := 1 ~ define b := byte:number i ~ i");
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
		test(1, "define.global a := [0, 1, 2,] ~ a [1]");
		test(1, "define { a: 1, b: (() => me/a), c: 3, } ~ b ()");
		test(4, "define (a, b) := (3, 4) ~ b");
		test(4, "define [a, b,] := [3, 4,] ~ b");
		test(4, "define { a: av, b: bv, } := { a: 3, b: 4, } ~ bv");

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
		test(160, "5 shl 5");
	}

	@Test
	public void testFold() {
		test(102, "let inc := 3 ~ fold (i := 0 # i < 100 # i + inc # i)");
		test(100, "fold ((a, b) := (0, 0) # a < 100 # (a + 1, b + 1) # a)");
		test(100, "fold ([a, b,] := [0, 0,] # b < 100 # [a + 1, b + 1,] # b)");
	}

	@Test
	public void testGlobal() {
		test(0, "" //
				+ "let module := \n" //
				+ "	let.global f () := 0 ~ \n" // global required
				+ "	let g () := f () ~ \n" //
				+ "	{ g, } \n" //
				+ "~ \n" //
				+ "() | module/g");

		test(9, "define.global max (a, b) := if (a < b) then b else a ~ max (8, 9)");
	}

	@Test
	public void testIo() {
		test(1, "do! 1");
	}

	@Test
	public void testLambda() {
		test(2, "() | (() => 2)");
		test(1, "0 | (a => a + 1)");
		test(3, "1, 2 | ((a, b) => a + b)");
		test(3, "[1, 2,] | ([a, b,] => a + b)");
		test(2, "let f := { a, b: v, c, } => v ~ f { a: 1, b: 2, c: 3, }");
		test(2, "let f := { b: v, } => v ~ f { a: 1, b: 2, c: 3, }");
		test(2, "let f := [a, b, c,] => b ~ f [1, 2, 3,]");
		test(6, "let g := glob (a => a + 1) ~ 3 | g | g | g");
		test(6, "define.function f a := a + 1 ~ 3 | f | f | f");
	}

	@Test
	public void testLambdaReturn() {
		test(2, "1 | (0 | (a => b => b + 1))");
	}

	@Test
	public void testLet() {
		test(4, "let i := 3 ~ i + 1");
		test(4, "let f i := i + 1 ~ 3 | f");
		test(1, "let.global a := [0, 1, 2,] ~ a [1]");
		test(1, "let { a: 1, b: (() => me/a), c: 3, } ~ b ()");
		test(4, "let (a, b) := (3, 4) ~ b");
		test(4, "let [a, b,] := [3, 4,] ~ b");
		test(4, "let { a: av, b: bv, } := { a: 3, b: 4, } ~ bv");
	}

	@Test
	public void testNew() {
		test(123, "do! (\n" //
				+ "	let p := !new^ _ ~ \n" //
				+ "	!assign p* := 123 ~ \n" //
				+ "	let v := p* ~ \n" //
				+ "	!delete^ p ~ v \n" //
				+ ")");
		test(456, "do! (\n" //
				+ "	let p := !new^ 456 ~ \n" //
				+ "	let v := p* ~ \n" //
				+ "	!delete^ p ~ v \n" //
				+ ")");
	}

	@Test
	public void testRecurse() {
		test(3, "define { dec n := if (3 < n) then (dec (n - 1)) else n ~ } ~ 3999 | dec");
		test(89, "define { fib n := if (1 < n) then (fib (n - 1) + fib (n - 2)) else 1 ~ } ~ 10 | fib");
	}

	@Test
	public void testReference() {
		test(5, "define i := 3 ~ define p := address.of i ~ 2 + p*");
	}

	@Test
	public void testReturnArray() {
		test(2, "define.function f i := [0, 1, i,] ~ (predef (f 2)) [2]");
	}

	@Test
	public void testSeq() {
		test(3, "0; 1; 2; 3");
	}

	@Test
	public void testSignExtension() {
		test(-1, "do! !asm (EAX = number:byte byte -1;) { SHR (EAX, 16); }/EAX");
		if (Funp_.isAmd64) {
			test(-1, "do! !asm (RAX = numberp:byte byte -1;) { SHR (RAX, 32); }/EAX");
			test(-1, "do! !asm (RAX = numberp:number -1;) { SHR (RAX, 32); }/EAX");
		}
	}

	@Test
	public void testString() {
		test(65, "define s := \"A world for us\" ~ number:byte s* [0]");
		test(65, "let.global s := \"A world for us\" ~ number:byte s* [0]");
	}

	@Test
	public void testStruct() {
		test(3, "define s := { a: 1, b: 2, c: 3, } ~ s/c");
		test(3, "define p := address.of predef { a: 1, b: 2, c: 3, } ~ p*/c");
	}

	@Test
	public void testTag() {
		test(3, "if (`t:v` = t:3) then v else 0");
		test(3, "define d := t:3 ~ if (`t:v` = d) then v else 0");
		test(0, "let d := s:() ~ type d = t:3 ~ if (`t:v` = d) then v else 0");
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
