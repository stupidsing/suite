package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.Suite;
import org.suite.node.Node;

public class FunCompilerTypeTest {

	private static final String variant = "" //
			+ "define type (A %) of (t,) >> \n" //
			+ "define type (B number %) of (t,) >> \n" //
			+ "define type (C boolean %) of (t,) >> \n";

	@Test
	public void testBasic() {
		assertEquals(Suite.parse("boolean") //
				, getType("4 = 8"));
	}

	@Test
	public void testDefineType() {
		getType("define type (KG number %) of (weight,) >> \n" //
				+ "let v = type weight (KG 1 %) >> \n" //
				+ "v = KG 99 %");
		getType("repeat {23}");
	}

	@Test
	public void testFail() {
		String cases[] = { "1 + \"abc\"" //
				, "(f => f {0}) | 1" //
				, "define fib = (i2 => dummy => 1, fib {i2}) >> ()" //
				, "define type (BTREE t t) of (btree,) >> \n" //
						+ "let v = type btree (BTREE 2 3) >> 1" //
				, variant + "A 4" //
				, variant + "B" //
		};

		// There is a problem in deriving type of 1:(fib {i2})...
		// Rule specified that right hand side of CONS should be a list,
		// however fib {i2} is a closure.
		for (String c : cases)
			getTypeMustFail(c);
	}

	@Test
	public void testFun() {
		assertEquals(Suite.parse("number => number") //
				, getType("a => a + 1"));
		assertEquals(Suite.parse("number") //
				, getType("define f = (a => a + 1) >> f {3}"));
		assertEquals(Suite.parse("boolean => boolean => boolean") //
				, getType("and"));
		assertEquals(Suite.parse("number => list-of number") //
				, getType("v => v, reverse {1,}"));
	}

	@Test
	public void testInstance() {
		String define = " \n" //
				+ "define type (NODE :t linked-list/:t %) of (linked-list/:t,) for any (:t,) >> \n" //
				+ "define type (NIL %) of (linked-list/:t,) for any (:t,) >> \n";

		getType(define + "NIL %");
		getType(define + "NODE false (NIL %) %");

		assertEquals(Suite.parse("boolean"), getType(define //
				+ "let n = NODE true (NIL %) % >> \n" //
				+ "NODE false n % = NIL %"));
		getTypeMustFail(define //
				+ "NODE 1 n % = NODE false n %");
		getTypeMustFail(define //
				+ "let n = NODE true (NIL %) % >> \n" //
				+ "NODE 1 n % = NIL %");
	}

	@Test
	public void testList() {
		assertEquals(Suite.parse("list-of number") //
				, getType("1,"));
		assertEquals(Suite.parse("list-of (list-of number)") //
				, getType("\"a\", \"b\", \"c\", \"d\","));
	}

	@Test
	public void testOneOf() {
		getType("" //
				+ "define type (NIL %) of (t,) >> \n" //
				+ "define type (BTREE t t %) of (t,) >> \n" //
				+ "let u = type t (NIL %) >> \n" //
				+ "let v = type t (NIL %) >> \n" //
				+ "v = BTREE (BTREE (NIL %) (NIL %) %) (NIL %) %");
	}

	@Test
	public void testTuple() {
		getType(variant + "A %");
		getType(variant + "B 4 %");
		getType(variant + "C true %");
		getType(variant + "if true then (A %)" //
				+ " else-if true then (B 3 %)" //
				+ " else (C false %)");
		getType("define type (BTREE number number %) of (btree,) >> \n"
				+ "BTREE 2 3 % = BTREE 4 6 %");
		getTypeMustFail("define type (T1 number number %) of (t1,) >> \n"
				+ "define type (T2 number number %) of (t2,) >> \n"
				+ "T1 2 3 % = T2 2 3 %");
		getTypeMustFail("define type (BTREE number number %) of (btree,) >> \n"
				+ "BTREE 2 3 % = BTREE \"a\" 6 %");
	}

	private static void getTypeMustFail(String c) {
		try {
			getType(c);
		} catch (RuntimeException ex) {
			return;
		}
		throw new RuntimeException("Cannot catch type error of: " + c);
	}

	private static Node getType(String a) {
		return Suite.evaluateFunType(a);
	}

}
