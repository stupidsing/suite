import java.io.IOException;

import org.junit.Test;

import suite.fp.eval.FunRbTreeTest;
import suite.instructionexecutor.InstructionTranslatorTest;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void test0() throws IOException {
		new FunRbTreeTest().test();
	}

	// /tmp/suite.instructionexecutor.InstructionTranslator/suite/instructionexecutor/TranslatedRun88.java:3161:
	// error: method invoke in class Invocable cannot be applied to given types;
	// f228_r1 = ((Invocable) data.getData()).invoke(this, Arrays.asList(n0,
	// n1));
	@Test
	public void test1() throws IOException {
		new InstructionTranslatorTest().testStandardLibrary();
	}

}
