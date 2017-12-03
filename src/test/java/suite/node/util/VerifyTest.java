package suite.node.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;

/**
 * TODO expand/unexpand definitions
 *
 * @author ywsing
 */
public class VerifyTest {

	@Test
	public void test() {
		Fun2<String, String, Definition> def = (t0, t1) -> new Definition(Suite.parse(t0), Suite.parse(t1));

		IMap<String, Definition> defs = IMap //
				.<String, Definition> empty() //
				.put("group", def.apply("group .isElem .zero .op", "" //
						+ ".isElem .zero, \n" //
						+ "is.op.binary .isElem .op, \n" //
						+ "(.isElem P0 => .op/.zero/P0 = .zero), \n" //
						+ "(.isElem P0, .isElem P1, .isElem P2 => .op/P0/(.op/P1/P2) = .op/(.op/P0/P1)/P2), \n" //
						+ "true"));

		IMap<String, Node> rules = IMap //
				.<String, Node> empty() //
				.put("and.0", Suite.parse("P, Q => P")) //
				.put("and.1", Suite.parse("P, Q, R => Q")) //
				.put("and.2", Suite.parse("P, Q, R, S => R")) //
				.put("and.3", Suite.parse("P, Q, R, S, T => S")) //
				.put("op1.0", Suite.parse("is.op.unitary P Op, IsElem P0 => IsElem Op/P0")) //
				.put("op2.0", Suite.parse("is.op.binary P Op, IsElem P1 => is.op.unitary IsElem Op/P1")) //
				.put("eq.0", Suite.parse("A = B => B = A")) //
				.put("eq.1", Suite.parse("A = B, B = C => A = C")) //
				.put("nat.0", Suite.parse("true => group is.nat 0 ADD")) //
				.put("nat.1", Suite.parse("is.nat N => is.nat (succ N)")) //
				.put("nat.add.0", Suite.parse("is.nat N => 0 + N = N")) //
				.put("nat.add.1", Suite.parse("is.nat N => M + succ N = succ (M + N)"));

		Sink2<String, String> test = (expect, proof) -> {
			Node expect_ = Suite.parse(expect);
			Node proven = new Verify(defs, rules).verify(Suite.parse(proof));
			System.out.println("proven :: " + proven);
			assertTrue(Binder.bind(expect_, proven, new Trail()));
		};

		String p = "lemma is.nat.zero := axiom nat.0 | expand group | choose is.nat >> ";

		test.sink2("is.nat 0", p + "is.nat.zero");
		test.sink2("is.nat succ 0", p + "is.nat.zero | satisfy (nat.1 {N:0})");
	}

	private class Definition {
		private Node t0;
		private Node t1;

		private Definition(Node t0, Node t1) {
			this.t0 = t0;
			this.t1 = t1;
		}
	}

	private class Verify {
		private IMap<String, Definition> defs;
		private IMap<String, Node> rules;

		private Verify(IMap<String, Definition> defs, IMap<String, Node> rules) {
			this.defs = defs;
			this.rules = rules;
		}

		private Node verify(Node proof) {
			Node[] m, m1;
			if ((m = Suite.match(".0, .1").apply(proof)) != null)
				return Tree.of(TermOp.AND___, verify(m[0]), verify(m[1]));
			else if ((m = Suite.match(".0 {}").apply(proof)) != null)
				return verify(m[0]);
			else if ((m = Suite.match(".0 {.1, .2}").apply(proof)) != null)
				return replace(verify(Suite.substitute(".0 {.1}", m[0], m[2])), m[1], new Reference());
			else if ((m = Suite.match(".0 {.1:.2}").apply(proof)) != null)
				return replace(verify(m[0]), m[1], m[2]);
			else if ((m = Suite.match("axiom .0").apply(proof)) != null)
				return verify(Suite.substitute("true | satisfy .0", m));
			else if ((m = Suite.match(".0 | choose .1").apply(proof)) != null) {
				Node list = verify(m[0]);
				Tree tree0, tree1;
				while (true)
					if ((tree0 = Tree.decompose(list, TermOp.AND___)) != null)
						if ((tree1 = Tree.decompose(tree0.getLeft(), TermOp.TUPLE_)) != null && tree1.getLeft() == m[1])
							return tree1;
						else
							list = tree0.getRight();
					else
						throw new RuntimeException();
			} else if ((m = Suite.match("contradict .0:.1 >> .2").apply(proof)) != null)
				if (Binder.bind(new Verify(defs, rules.put(name(m[0]), m[1])).verify(m[2]), Atom.FALSE, new Trail()))
					return Suite.substitute("not .0", m[1]);
				else
					throw new RuntimeException();
			else if ((m = Suite.match(".0 | expand .1").apply(proof)) != null) {
				Definition def = defs.get(name(m[1]));
				return replaceBind(verify(m[0]), def.t0, def.t1);
			} else if ((m = Suite.match("lemma .0 := .1 >> .2").apply(proof)) != null)
				return new Verify(defs, rules.put(name(m[0]), verify(m[1]))).verify(m[2]);
			else if ((m = Suite.match(".0 | rexpand .1").apply(proof)) != null) {
				Definition def = defs.get(name(m[1]));
				return replaceBind(verify(m[0]), def.t1, def.t0);
			} else if ((m = Suite.match(".0 | satisfy .1").apply(proof)) != null)
				if ((m1 = Suite.match(".0 => .1").apply(verify(m[1]))) != null && Binder.bind(verify(m[0]), m1[0], new Trail()))
					return m1[1];
				else
					throw new RuntimeException();
			else if ((m = Suite.match("true").apply(proof)) != null)
				return Atom.TRUE;
			else if (proof instanceof Atom)
				return rules.get(name(proof));
			else
				throw new RuntimeException("cannot verify " + proof);
		}

		private Node replaceBind(Node node, Node from, Node to) {
			return new Object() {
				private Node replace(Node node_) {
					Generalizer generalizer = new Generalizer();
					Node t0 = generalizer.generalize(from);
					Node t1 = generalizer.generalize(to);
					Trail trail = new Trail();

					if (Binder.bind(node_, t0, trail))
						return t1;
					else
						trail.unwindAll();

					Tree tree = Tree.decompose(node_);

					return tree != null //
							? Tree.of(tree.getOperator(), replace(tree.getLeft()), replace(tree.getRight())) //
							: node_;
				}
			}.replace(node);
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

		private String name(Node node) {
			return ((Atom) node).name;
		}
	}

}
