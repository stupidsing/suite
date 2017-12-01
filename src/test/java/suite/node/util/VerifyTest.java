package suite.node.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil2.Sink2;

public class VerifyTest {

	@Test
	public void test() {
		IMap<String, Node> rules = IMap //
				.<String, Node>empty() //
				.put("and.0", Suite.parse("P, Q => P")) //
				.put("and.1", Suite.parse("P, Q => Q")) //
				.put("op1.0", Suite.parse("is.op1 P Op, P P0 => P (Op P0)")) //
				.put("op2.0", Suite.parse("is.op2 P Op, P P1 => is.op1 P (Op P1)")) //
				.put("eq.0", Suite.parse("A = B => B = A")) //
				.put("eq.1", Suite.parse("A = B, B = C => A = C")) //
				.put("grp.0", Suite.parse("is.group P Zero Op => P Zero")) //
				.put("grp.1", Suite.parse("is.group P Zero Op => is.op2 P Op")) //
				.put("grp.2", Suite.parse("is.group P Zero Op, P P0 => Op Zero P0 = Zero")) //
				.put("grp.3", Suite.parse("is.group P Zero Op, P P0, P P1, P P2 => Op P0 (Op P1 P2) = Op (Op P0 P1) P2")) //
				.put("nat.0", Suite.parse("true => is.group is.nat 0 ADD")) //
				.put("nat.1", Suite.parse("is.nat N => is.nat (succ N)")) //
				.put("nat.add.0", Suite.parse("is.nat N => 0 + N = N")) //
				.put("nat.add.1", Suite.parse("is.nat N => M + succ N = succ (M + N)"));

		Sink2<String, String> test = (expect, proof) -> {
			Node expect_ = Suite.parse(expect);
			Node proven = new Verify(rules).verify(Suite.parse(proof));
			System.out.println("proven :: " + proven);
			assertTrue(Binder.bind(expect_, proven, new Trail()));
		};

		String p = "lemma is.nat.zero := axiom nat.0 | satisfy (grp.0 {P:is.nat} {Zero:0} {Op:ADD}) >> ";

		test.sink2("is.nat 0", p + "is.nat.zero");
		test.sink2("is.nat succ 0", p + "is.nat.zero | satisfy (nat.1 {N:0})");
	}

	private class Verify {
		private IMap<String, Node> rules;

		private Verify(IMap<String, Node> rules) {
			this.rules = rules;
		}

		private Node verify(Node proof) {
			Node[] m, m1;
			if ((m = Suite.match(".0, .1").apply(proof)) != null)
				return Tree.of(TermOp.AND___, verify(m[0]), verify(m[1]));
			else if ((m = Suite.match(".0 {.1:.2}").apply(proof)) != null)
				return replace(verify(m[0]), m[1], m[2]);
			else if ((m = Suite.match("axiom .0").apply(proof)) != null)
				return verify(Suite.substitute("true | satisfy .0", m));
			else if ((m = Suite.match("contradict .0:.1 >> .2").apply(proof)) != null)
				if (Binder.bind(new Verify(rules.put(((Atom) m[0]).name, m[1])).verify(m[2]), Atom.FALSE, new Trail()))
					return Suite.substitute("not .0", m[1]);
				else
					throw new RuntimeException();
			else if ((m = Suite.match("lemma .0 := .1 >> .2").apply(proof)) != null)
				return new Verify(rules.put(((Atom) m[0]).name, verify(m[1]))).verify(m[2]);
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
				throw new RuntimeException("cannot verify " + proof);
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
