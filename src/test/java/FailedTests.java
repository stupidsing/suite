import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.eval.FunRbTreeTest;
import suite.instructionexecutor.InstructionTranslatorTest;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void test0() throws IOException {
		new FunRbTreeTest().test();
	}

	// UnsupportedOperationException
	@Test
	public void test1() throws IOException {
		new InstructionTranslatorTest().testAtomString();
	}

	// Cyclic types
	@Test
	public void test2() {
		Suite.evaluateFunType("define f = (v => (v;) = v) >> f");
	}

}
