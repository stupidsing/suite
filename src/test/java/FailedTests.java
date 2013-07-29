import java.io.IOException;

import org.junit.Test;

import suite.instructionexecutor.FunRbTreeTest;
import suite.lp.Suite;

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

	// Strange error message "Unknown expression (temp$$4 => temp$$4 {})"
	@Test
	public void test2() throws IOException {
		Suite.evaluateFun("`{}`", false);
	}

}
