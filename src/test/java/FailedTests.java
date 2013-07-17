import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.instructionexecutor.FunRbTreeTest;
import org.junit.Test;
import org.suite.Suite;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void test0() throws IOException {
		new FunRbTreeTest().test();
	}

	// Strange error message "Unknown expression if b"
	@Test
	public void test1() throws IOException {
		Suite.evaluateFun("if a then b", false);
	}

	// Should be something more complicated than an unbounded type
	@Test
	public void test2() {
		String t = Suite.evaluateFunType( //
				"using RB-TREE >> 0 until 10 | map {add-key-value/ {1}} | apply | {EMPTY}").toString();
		System.out.println(t);
		assertNotEquals('.', t.charAt(0));
	}

	// Reduced case of test2()
	@Test
	public void test3() {
		String s = "" //
				+ "define type EMPTY of (clazz/:t,) for any (:t,) >>\n" //
				+ "define add = type (:t :- clazz/:t => clazz/:t) (a => a) >>\n" //
				+ "add; | _lhead | {EMPTY}\n";
		System.out.println(s);

		String t = Suite.evaluateFunType(s).toString();
		System.out.println(t);
		assertNotEquals('.', t.charAt(0));
	}

}
