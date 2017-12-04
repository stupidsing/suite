package suite.node.util;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IList;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.os.LogUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil2.Fun2;

public class VerifyTest {

	@Test
	public void test() {
		Fun<IList<String>, Node> and = new Fun<>() {
			public Node apply(IList<String> list) {
				return !list.isEmpty() ? Tree.of(TermOp.AND___, Suite.parse(list.head), apply(list.tail)) : Atom.TRUE;
			}
		};

		Fun2<String, Node, Definition> defn = (t0, t1) -> new Definition(Suite.parse(t0), t1);
		Fun2<String, String, Definition> def2 = (t0, t1) -> defn.apply(t0, Suite.parse(t1));

		IMap<String, Definition> defs = IMap //
				.<String, Definition> empty() //
				.put("def$eq", defn.apply("eq-class .eq", and.apply(IList.asList( //
						"commute # A .eq B => B .eq A", //
						"transit # A .eq B, B .eq C => A .eq C")))) //
				.put("def$uni-op", def2.apply("uni-op .isElem .op", ".isElem P => .isElem (.op P)")) //
				.put("def$bin-op", def2.apply("bin-op .isElem .op", ".isElem P, .isElem Q => .isElem (P .op Q)")) //
				.put("def$group", defn.apply("group .isElem .eq .op .inv .zero", and.apply(IList.asList( //
						".isElem .zero", //
						"eq-class .eq", //
						"bin-op .isElem .op", //
						".isElem P => (.zero .op P) .eq P", //
						".isElem P, .isElem Q, .isElem R => (P .op (Q .op R)) .eq ((P .op Q) .op R)", //
						".isElem P => (P .op (.inv P)) .eq .zero")))) //
				.put("def$field", defn.apply("field .isElem .eq .op0 .inv0 .zero .op1 .inv1 .one", and.apply(IList.asList( //
						"group .isElem .eq .op0 .inv0 .zero", //
						".isElem .one", //
						"bin-op .op1", //
						".isElem .P => (.one .op1 P) .eq P", //
						".isElem P, .isElem Q, .isElem R => (P .op1 (Q .op1 R)) .eq ((P .op1 Q) .op1 R)", //
						".isElem P => P .eq .zero; (P .op1 (.inv1 P)) .eq .one"))));

		IMap<String, Node> axioms = IMap //
				.<String, Node> empty() //
				.put("@not.0", Suite.parse("P, not P => false")) //
				.put("@and.0", Suite.parse("P, Q => P")) //
				.put("@and.1", Suite.parse("P, Q => Q")) //
				.put("@or.0", Suite.parse("P => P; Q")) //
				.put("@or.1", Suite.parse("Q => P; Q")) //
				.put("@nat.0", Suite.parse("true => group is-nat nat-eq nat-add nat-neg 0")) //
				.put("@nat.1", Suite.parse("is-nat N => is-nat (succ N)")) //
				.put("@nat-add.0", Suite.parse("is-nat N => (0 nat-add N) nat-eq N")) //
				.put("@nat-add.1", Suite.parse("is-nat N => M nat-eq (succ N) = succ (M nat-add N)"));

		new Verify(defs, axioms) //
				.extend("given @cond.0 := eq-class Eq >> " //
						+ "given @cond.1 := P Eq Q >> " //
						+ "given @cond.2 := not (Q Eq R) >> " //
						+ "contradict @fail := P Eq R >> " //
						+ "lemma !Eq := @cond.0 | expand def$eq >> " //
						+ "lemma !Q-Eq-P := !Eq | choose commute | rename {A, B,} | rsatisfy @cond.1 >> " //
						+ "lemma !Q-Eq-R := !Q-Eq-P, @fail | satisfy (!Eq | choose transit | rename {A, B, C,}) >> " //
						+ "!Q-Eq-R, @cond.2 | satisfy (@not.0 | rename {P,})") //
				.extend("is-nat 0", "axiom @nat.0 | expand def$group | choose {is-nat _}") //
				.extend("is-nat (succ 0)", "'is-nat 0' | satisfy (@nat.1 | rename {N:0})");
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
			else if ((m = Suite.match(".0 # .1").apply(proof)) != null)
				return Tree.of(TermOp.NEXT__, m[0], verify(m[1]));
			else if ((m = Suite.match("axiom .0").apply(proof)) != null)
				return verify(Suite.substitute("true | satisfy .0", m));
			else if ((m = Suite.match(".0 | choose {.1}").apply(proof)) != null) {
				Node list = verify(m[0]);
				for (Node node : Tree.iter(list, TermOp.AND___))
					if (Binder.bind(node, new Generalizer().generalize(m[1]), new Trail()))
						return node;
				throw new RuntimeException("cannot verify " + proof);
			} else if ((m = Suite.match(".0 | choose .1").apply(proof)) != null) {
				Node list = verify(m[0]);
				Tree tree;
				for (Node node : Tree.iter(list, TermOp.AND___))
					if ((tree = Tree.decompose(node, TermOp.NEXT__)) != null && tree.getLeft() == m[1])
						return tree.getRight();
				throw new RuntimeException("cannot verify " + proof);
			} else if ((m = Suite.match("contradict .0 := .1 >> .2").apply(proof)) != null)
				if (Binder.bind(new Verify(defs, rules.put(name(m[0]), m[1])).verify(m[2]), Atom.FALSE, new Trail()))
					return Suite.substitute("not .0", m[1]);
				else
					throw new RuntimeException("cannot verify " + proof);
			else if ((m = Suite.match(".0 | expand .1").apply(proof)) != null) {
				Definition def = defs.get(name(m[1]));
				return replaceBind(verify(m[0]), def.t0, def.t1);
			} else if ((m = Suite.match("given .0 := .1 >> .2").apply(proof)) != null)
				return Suite.substitute(".0 => .1", m[1], new Verify(defs, rules.put(name(m[0]), m[1])).verify(m[2]));
			else if ((m = Suite.match("lemma .0 := .1 >> .2").apply(proof)) != null)
				return new Verify(defs, rules.put(name(m[0]), verify(m[1]))).verify(m[2]);
			else if ((m = Suite.match(".0 | rename {}").apply(proof)) != null)
				return verify(m[0]);
			else if ((m = Suite.match(".0 | rename {.1, .2}").apply(proof)) != null)
				return replace(verify(Suite.substitute(".0 | rename {.1}", m[0], m[2])), m[1], new Reference());
			else if ((m = Suite.match(".0 | rename {.1:.2}").apply(proof)) != null)
				return replace(verify(m[0]), m[1], m[2]);
			else if ((m = Suite.match(".0 | rexpand .1").apply(proof)) != null) {
				Definition def = defs.get(name(m[1]));
				return replaceBind(verify(m[0]), def.t1, def.t0);
			} else if ((m = Suite.match(".0 | rsatisfy .1").apply(proof)) != null)
				return verify(Suite.substitute(".0 | satisfy .1", m[1], m[0]));
			else if ((m = Suite.match(".0 | satisfy .1").apply(proof)) != null)
				if ((m1 = Suite.match(".0 => .1").apply(verify(m[1]))) != null && Binder.bind(verify(m[0]), m1[0], new Trail()))
					return m1[1];
				else
					throw new RuntimeException("cannot verify " + proof);
			else if ((m = Suite.match("true").apply(proof)) != null)
				return Atom.TRUE;
			else if (proof instanceof Atom)
				return rules.get(name(proof));
			else
				throw new RuntimeException("cannot verify " + proof);
		}

		public Verify extend(String lemma, String proof) {
			Node node = extend_(proof);
			if (Binder.bind(Suite.parse(lemma), node, new Trail()))
				return new Verify(defs, rules.put(lemma, node));
			else
				throw new RuntimeException();
		}

		public Verify extend(String proof) {
			Node node = extend_(proof);
			return new Verify(defs, rules.put(Formatter.dump(node), node));
		}

		private Node extend_(String proof) {
			Node node = verify(Suite.parse(proof));
			LogUtil.info("proven :: " + node);
			return node;
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
