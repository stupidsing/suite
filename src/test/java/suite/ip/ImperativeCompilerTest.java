package suite.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.primitive.Bytes;

public class ImperativeCompilerTest {

	@Test
	public void test() {
		Bytes bytes = new ImperativeCompiler().compile(0, "`0` = 1;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

}
