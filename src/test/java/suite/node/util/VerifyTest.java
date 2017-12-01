package suite.node.util;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;

public class VerifyTest {

	@Test
	public void test() {
		IMap<String, Node> rules = IMap //
				.<String, Node>empty() //
				.put("nat.0", Suite.parse("true => is-nat 0")) //
				.put("nat.1", Suite.parse("is-nat N => is-nat (succ N)"));

		System.out.println(new Verify(rules).verify(Suite.parse("axiom nat.0 | satisfy (nat.1 | subst N 0)")));
	}

	private class Verify {
		private IMap<String, Node> rules;

		private Verify(IMap<String, Node> rules) {
			this.rules = rules;
		}

		private Node verify(Node proof) {
			Node[] m, m1;
			if ((m = Suite.match(".0 && .1").apply(proof)) != null)
				return Tree.of(TermOp.BIGAND, verify(m[0]), verify(m[1]));
			else if ((m = Suite.match("axiom .0").apply(proof)) != null)
				return verify(Suite.substitute("true | satisfy .0", m));
			else if ((m = Suite.match("lemma .0 := .1 >> .2").apply(proof)) != null)
				return new Verify(rules.put(((Atom) m[0]).name, verify(m[1]))).verify(m[2]);
			else if ((m = Suite.match(".0 | subst .1 .2").apply(proof)) != null)
				return replace(verify(m[0]), m[1], m[2]);
			else if ((m = Suite.match(".0 | satisfy .1").apply(proof)) != null)
				if ((m1 = Suite.match(".0 => .1").apply(verify(m[1]))) != null && Binder.bind(verify(m[0]), m1[0], new Trail()))
					return m1[1];
				else
					throw new RuntimeException();
			else if ((m = Suite.match("true").apply(proof)) != null)
				return Atom.TRUE;
			else if (proof instanceof Atom)
				return rules.get(((Atom) proof).name);
			else
				throw new RuntimeException();
		}

		private Node replace(Node node, Node from, Node to) {
			return new Object() {
				private Node replace(Node node_) {
					Tree tree;
					if ((tree = Tree.decompose(node_)) != null)
						return Tree.of(tree.getOperator(), replace(tree.getLeft()), replace(tree.getRight()));
					else
						return node_ == from ? to : node_;
				}
			}.replace(node);
		}
	}

}
