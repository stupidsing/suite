package suite.node.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;

public class VerifyTest {

	private class Rule {
		private Node t0, t1;
	}

	@Test
	public void test() {
		new Verify(new HashMap<>()).verify(null);
	}

	private class Verify {
		private Map<String, Rule> rules;

		private Verify(Map<String, Rule> rules) {
			this.rules = rules;
		}

		private Node verify(Node proof) {
			Node[] m;
			if ((m = Suite.match(".0 && .1").apply(proof)) != null)
				return Tree.of(TermOp.BIGAND, verify(m[0]), verify(m[1]));
			else if ((m = Suite.match("axiom .0").apply(proof)) != null)
				return verify(Suite.substitute("true | imply .0", m));
			else if ((m = Suite.match(".0 | imply .1").apply(proof)) != null) {
				Rule rule = rules.get(((Atom) m[1]).name);
				Generalizer generalizer = new Generalizer();
				bind(m[0], generalizer.generalize(rule.t0));
				return generalizer.generalize(rule.t1);
			} else if ((m = Suite.match(".0 | subst .1 .2").apply(proof)) != null) {
				Generalizer generalizer = new Generalizer();
				Node node = generalizer.generalize(m[0]);
				bind(generalizer.generalize(m[1]), m[2]);
				return node;
			} else if ((m = Suite.match("true").apply(proof)) != null)
				return Atom.TRUE;
			else
				throw new RuntimeException();
		}

		private void bind(Node n0, Node n1) {
			if (!Binder.bind(n0, n1, new Trail()))
				throw new RuntimeException();
		}
	}

}
