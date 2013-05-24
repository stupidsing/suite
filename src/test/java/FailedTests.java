import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Node;

public class FailedTests {

	@Test
	public void test0() throws IOException { // not balanced
		RuleSet rs = RuleSetUtil.create();
		Suite.importResource(rs, "auto.sl");
		Suite.importResource(rs, "23t.sl");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 32; i++)
			sb.append(i + ", ");

		assertTrue(Suite.proveThis(rs, "" //
				+ "23t-add-list (" + sb + ") T/.t \n" //
				+ ", pretty.print .t, nl, dump .d, nl"));
	}

	@Test
	public void test1() throws IOException { // not balanced
		RuleSet rs = RuleSetUtil.create();
		Suite.importResource(rs, "auto.sl");
		Suite.importResource(rs, "23t.sl");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 32; i++)
			sb.append(i + ", ");

		String list = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,";
		assertTrue(Suite.proveThis(rs, "" //
				+ "23t-add-list (" + list + ") T/.t" //
				+ ",  pretty.print .t, nl, dump .d, nl"));
	}

	@Test
	public void test2() { // takes very long
		eval("" //
				+ "define type (A %) of (t,) >> \n" //
				+ "define type (B %) of (t,) >> \n" //
				+ "define type (C %) of (t,) >> ( \n" //
				+ "    ((A %):1:, (A %):2:,), \n" //
				+ "    ((B %):1:, (B %):2:,), \n" //
				+ "    ((C %):1:, (C %):2:,), \n" //
				+ ")");
	}

	@Test
	public void test3() { // test3 and test4 can only live one; change
							// LogicInstructionExecutor.java:69
		Suite.evaluateLogical( //
				Suite.parse("(.a = 1; .a = 2), !, .a = 1") //
				, Atom.NIL //
				, new ProverConfig() //
				, true);
	}

	@Test
	public void test4() {
		Suite.evaluateLogical( //
				Suite.parse("(a#) >> a, !") //
				, Atom.NIL //
				, new ProverConfig() //
				, true);
	}

	@Test
	public void test5() {
		Suite.evaluateLogical( //
				Suite.parse(".c0 = .c1, lc-parse () .call1 _, !, dump .call1, nl") //
				, Atom.NIL //
				, new ProverConfig() //
				, true);
	}

	private static Node eval(String f) {
		return Suite.evaluateEagerFun(f);
	}

}
