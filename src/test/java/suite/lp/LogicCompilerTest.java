package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.Verbs.ReadString;
import suite.Suite;

public class LogicCompilerTest {

	@Test
	public void test() {
		assertTrue(prove("()"));
		assertTrue(prove("3 = 3"));
		assertFalse(prove("fail"));
		assertFalse(prove("1 = 2"));
	}

	@Test
	public void testAuto() { 
		var preds = ReadString.from("src/main/ll/auto.sl");
		String goal = "(" + preds + ") ~ member (a, b, c,) c";
		assertTrue(Suite.proveLogic(goal));
	}

	@Test
	public void testCut() {
		assertTrue(prove("(.a = 1; .a = 2), !, .a = 1"));
		assertFalse(prove("(.a = 1; .a = 2), !, .a = 2"));
		assertFalse(prove(".a = 1, !, .b = 2, fail; .b = 3"));
		assertTrue(prove("(a .b :- !, .b = 1 #) ~ (.b = 1; .b = 2), a .b"));
		assertTrue(prove("(a :- fail # a :- ! #) ~ a"));
		assertTrue(prove("(a :- fail, ! # a #) ~ a"));
		assertFalse(prove("(a :- !, fail # a #) ~ a"));
		assertTrue(prove("(a#) ~ a, !"));
	}

	@Test
	public void testEval() {
		assertTrue(prove("3 < 4"));
		assertTrue(prove("3 <= 4"));
		assertFalse(prove("5 < 4"));
		assertFalse(prove("4 <= 3"));
	}

	@Test
	public void testFibonacci() {
		assertTrue(prove("" //
				+ "( \n" //
				+ "    fib 0 1 :- ! # \n" //
				+ "    fib 1 1 :- ! # \n" //
				+ "    fib .n .f \n" //
				+ "        :- let .n1 (.n - 1) \n" //
				+ "        , let .n2 (.n1 - 1) \n" //
				+ "        , fib .n1 .f1 \n" //
				+ "        , fib .n2 .f2 \n" //
				+ "        , let .f (.f1 + .f2) \n" //
				+ "    # \n" //
				+ ") ~ fib 10 89"));
	}

	@Test
	public void testLogic() {
		assertTrue(prove("1 = 2; 3 = 3"));
		assertFalse(prove("3 = 3, 1 = 2"));
	}

	@Test
	public void testNot() {
		assertTrue(prove("not (1 = 2)"));
		assertFalse(prove("not (1 = 1)"));
		assertTrue(prove("not (not (.v = 1)), .v = 2"));
		assertFalse(prove("not (not (.v = 1)), 1 = 2"));
	}

	@Test
	public void testOnce() {
		assertTrue(prove("once (.v = 1; .v = 2), .v = 1"));
		assertFalse(prove("once (.v = 1; .v = 2), .v = 2"));
	}

	@Test
	public void testOrBinds() {
		prove("(fail; .b = 1), .b = 2, yes");
		prove("(yes; .b = 1), .b = 2, fail");
	}

	@Test
	public void testTailRecursion() {
		assertTrue(prove("(dec 0 :- ! # dec .n :- let .n1 (.n - 1), dec .n1 #) ~ dec 65536"));
		assertTrue(prove("(dec 0 :- ! # dec .n :- let .n1 (.n - 1), dec .n1, ! #) ~ dec 65536"));
	}

	@Test
	public void testVariables() {
		assertTrue(prove(".a = 1, 1 = .a"));
		assertFalse(prove(".a = 1, .a = 2"));
	}

	@Test
	public void testWith() {
		assertTrue(prove("(p 2 # p 3 #) ~ p .v, .v = 3"));
		assertFalse(prove("(p 2 :- ! # p 3 #) ~ p .v, .v = 3"));
		assertTrue(prove("(p .v :- q .v # q 3 #) ~ p 3"));
	}

	private boolean prove(String program) {
		return Suite.proveLogic(program);
	}

}
