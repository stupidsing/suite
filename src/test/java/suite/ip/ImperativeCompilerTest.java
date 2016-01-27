package suite.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.primitive.Bytes;

public class ImperativeCompilerTest {

	private ImperativeCompiler imperativeCompiler = new ImperativeCompiler();

	@Test
	public void testArray() {
		Bytes bytes = imperativeCompiler.compile(0, "declare array as (int * 2) = array (1, 2,); let array/:3 = array/:4;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testDataStructure() {
		String s = "" //
				+ "constant p = fix :p struct (+next as pointer:(:p),);" //
				+ "declare pnext = function [e as pointer:p,] e/*/+next;" //
				+ "declare object = new p (+next = null,);" //
				+ "let object/+next = pnext [& object,];" //
				+ "0";
		Bytes bytes = imperativeCompiler.compile(0, s);
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
		Bytes bytes = imperativeCompiler.compile(0, "null/* + 1 = 2;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testField() {
		Bytes bytes = imperativeCompiler.compile(0, "declare x as struct (+i as int, +j as int,); x/+j = 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testJump() {
		Bytes bytes = imperativeCompiler.compile(0, "if (2 > 1) then 1 else 0;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testLet() {
		Bytes bytes = imperativeCompiler.compile(0, "let null/* = 1 shl 3;");
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
