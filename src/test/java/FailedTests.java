import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunRbTreeTest;
import suite.funp.Funp_;
import suite.http.Crypts;
import suite.ip.ImperativeCompiler;

public class FailedTests {

	// cannot understand the error message
	// "Cannot resolve type of (IF (TYPE-CAST I32 VAR f) (NUMBER 1) BOOLEAN 0)"
	@Test
	public void testAssignWrongSize() {
		new ImperativeCompiler().compile(0, "declare (function [] int) f = function [] 0; (f as int && 1);");
	}

	// shall we support this?
	@Test
	public void testClassOfClass() {
		assertEquals(Suite.parse("C2 boolean"), Suite.evaluateFunType("" //
				+ "data (C0 :t) over :t as A :t ~ \n" //
				+ "data (C1 :t) over :t as (C0 :t) ~ \n" //
				+ "data (C2 :t) over :t as (C1 :t) ~ \n" //
				+ "(C2 boolean) of (A true)"));
	}

	@Test
	public void testCrypts() {
		var rsa = new Crypts().rsa("");
		assertEquals(rsa.encrypt(new byte[0]), rsa.encrypt(new byte[0]));
	}

	// cannot capture reference to a structure
	@Test
	public void testDataStructure() {
		var bytes = new ImperativeCompiler().compile(0, "" //
				+ "constant p = fix :p struct (() | pointer::p +next);" //
				+ "declare r = & new p (+next = null,);" //
				+ "0");

		assertNotNull(bytes);
		System.out.println(bytes);
	}

	// duplicate symbols. Cannot bind again when using is used in a closure
	@Test
	public void testClosureUsing() {
		Suite.noLibraries(() -> Suite.evaluateFun("use MATH ~ (a => (use MATH ~ 1))_{0}", true));
	}

	// NPE. Method not found in concatm due to not importing standard library.
	// module dependency checks are necessary
	@Test
	public void testEager() {
		Suite.noLibraries(() -> Suite.evaluateFun("use MONAD ~ 0", false));
	}

	// why returning null pointer?
	@Test
	public void testRecursiveCall() {
		Suite.isInstructionDump = true;
		Suite.noLibraries(() -> {
			assertNotNull(Suite.evaluateFun("define f := f ~ f", true));
			return true;
		});
	}

	// takes forever to type check
	// @Test
	public void testRecursiveType() {
		Suite.evaluateFunType("data (rb-tree_{:t}) over :t as (rb-tree_{:t}) ~ (:t => rb-tree_{:t}) of 1");
	}

	// (Expected) infinite loop.
	// (Actual) short boolean evaluation in Prover skipped the loop:
	// alt = andTree(bt, orTree(andTree(right, rem), alt));
	@Test
	public void testRepeat() throws IOException {
		var rs = Suite.newRuleSet();
		rs.importPath("auto.sl");
		assertTrue(Suite.proveLogic(rs, "repeat, fail"));
	}

	// should we show "field not found" instead of "cannot get size of type xxx"?
	@Test
	public void testStruct() {
		Funp_.main(false).compile(0, "define s := { a: 1, } ~ s/b");
	}

	// takes 11 seconds to type check
	@Test
	public void testTypeCheck() throws IOException {
		var start = System.currentTimeMillis();
		new FunRbTreeTest().test();
		var end = System.currentTimeMillis();
		assertTrue(end - start < 1000l);
	}

}
