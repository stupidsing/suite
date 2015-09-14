package suite.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.primitive.Bytes;

public class ImperativeCompilerTest {

	private ImperativeCompiler imperativeCompiler = new ImperativeCompiler();

	@Test
	public void testDeclare() {
		Bytes bytes = imperativeCompiler.compile(0, "declare v = 1; let v = 2;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testExpr() {
		Bytes bytes = imperativeCompiler.compile(0, "`0` + 1 = 2;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testLet() {
		Bytes bytes = imperativeCompiler.compile(0, "let `0` = 1 shl 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testSubroutine() {
		Bytes bytes = imperativeCompiler.compile(0, "declare t = this; declare s = [i,] ( i; ); t:s [1,];");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

}
