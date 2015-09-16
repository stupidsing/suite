package suite.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.primitive.Bytes;

public class ImperativeCompilerTest {

	private ImperativeCompiler imperativeCompiler = new ImperativeCompiler();

	@Test
	public void testArray() {
		Bytes bytes = imperativeCompiler.compile(0, "declare array as (int * 16); let array:3 = array:4;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testDeclare() {
		Bytes bytes = imperativeCompiler.compile(0, "declare v = 1; let v = 2;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testExpr() {
		Bytes bytes = imperativeCompiler.compile(0, "`null` + 1 = 2;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testField() {
		Bytes bytes = imperativeCompiler.compile(0, "declare x as struct (i as int, j as int,); x^j = 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testLet() {
		Bytes bytes = imperativeCompiler.compile(0, "let `null` = 1 shl 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testSubroutine() {
		Bytes bytes = imperativeCompiler.compile(0, "declare s = function [i,] ( i; ); declare s1 = s; s1 [1,];");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

}
