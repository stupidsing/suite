package suite.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import primal.primitive.adt.Bytes;

public class ImperativeCompilerTest {

	private ImperativeCompiler imperativeCompiler = new ImperativeCompiler();

	@Test
	public void testArray() {
		var bytes = imperativeCompiler.compile(0, "declare (int * 2) array = array (1, 2,); {array/:3} = array/:4;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testBind() {
		var bytes = imperativeCompiler.compile(0, "if-bind (1 := $a) then a else 0");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testDataStructure() {
		var s = "" //
				+ "constant p = fix :p struct ( | pointer::p next);" //
				+ "declare pnext = function [pointer:p e,] e/*/next;" //
				+ "declare object = new p (next = null,);" //
				+ "{object/next} = pnext [& object,];" //
				+ "0";
		var bytes = imperativeCompiler.compile(0, s);
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testDataStructureType() {
		var s = "" //
				+ "declare object = new (prev = 0, next = 1,);" //
				+ "object/next";
		var bytes = imperativeCompiler.compile(0, s);
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testDeclare() {
		Bytes bytes = imperativeCompiler.compile(0, "declare v = 1; {v} = 2;");
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
		var bytes = imperativeCompiler.compile(0, "signature x = struct ( | int i | int j); x/j = 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testJump() {
		var bytes = imperativeCompiler.compile(0, "if (1 < 2) then 1 else 0;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testLet() {
		var bytes = imperativeCompiler.compile(0, "{null/*} = 1 shl 3;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testSubroutine() {
		var bytes = imperativeCompiler.compile(0, "declare s = function [i,] ( i; ); declare s1 = s; s1 [1,];");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	@Test
	public void testTag() {
		var s = "constant optional = (tag ( | 0 struct () | 1 int));" //
				+ "declare optional i = newt optional 0 new ();" //
				+ "declare optional j = newt optional 1 2;" //
				+ "0";

		var bytes = imperativeCompiler.compile(0, s);
		assertNotNull(bytes);
		System.out.println(bytes);
	}

}
