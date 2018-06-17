package suite.node.util;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IList;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.tree.TreeAnd;
import suite.os.LogUtil;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil2.Fun2;

/**
 * TODO ZFC
 * 
 * TODO define "there exists"
 * 
 * TODO define finite
 *
 * @author ywsing
 */
public class VerifyTest {

	@Test
	public void test() {
		var and = new Fun<IList<String>, Node>() {
			public Node apply(IList<String> list) {
				return !list.isEmpty() ? TreeAnd.of(Suite.parse(list.head), apply(list.tail)) : Atom.TRUE;
			}
		};

		Fun2<String, Node, Definition> def_ = (t0, t1) -> new Definition(Suite.parse(t0), t1);
		Fun2<String, String, Definition> def2 = (t0, t1) -> def_.apply(t0, Suite.parse(t1));
		Fun2<String, IList<String>, Definition> defn = (t0, t1) -> def_.apply(t0, and.apply(t1));

		var defs = IMap //
				.<String, Definition> empty() //
				.put("def$iff", defn.apply("iff .A .B", IList.of( //
						"fore # .A => .B", //
						"back # .B => .A"))) //
				.put("def$eq", defn.apply("eq-class .eq", IList.of( //
						"reflexive # true => .A .eq .A", //
						"symmetric # .A .eq .B => .B .eq .A", //
						"transitive # .A .eq .B, .B .eq .C => .A .eq .C"))) //
				.put("def$uni-op", def2.apply("uni-op .class .op", //
						".class .P => .class (.op .P)")) //
				.put("def$bin-op", def2.apply("bin-op .class .op", //
						".class .P, .class .Q => .class (.P .op .Q)")) //
				.put("def$set", defn.apply("set .eq", IList.of( //
						".S .eq .T => (.E set-in .S) iff (.E set-in .T)"))) //
				.put("def$group0", defn.apply("group0 .class .eq .op .zero", IList.of( //
						".class .zero", //
						"eq-class .eq", //
						".class .P, .P .eq .Q => .class .Q", //
						"bin-op .class .op", //
						".class .P => (.zero .op .P) .eq .P", //
						".class .P, .class .Q, .class .R => (.P .op (.Q .op .R)) .eq ((.P .op .Q) .op .R)"))) //
				.put("def$group", defn.apply("group .class .eq .op .inv .zero", IList.of( //
						"group0 .class .eq .op .zero", //
						".class .P => (.P .op (.inv .P)) .eq .zero"))) //
				.put("def$field", defn.apply("field .class .eq .op0 .inv0 .zero .op1 .inv1 .one", IList.of( //
						"group .class .eq .op0 .inv0 .zero", //
						"group0 .class .eq .op1 .inv1 .one", //
						".class .P => .P .eq .zero; (.P .op1 (.inv1 .P)) .eq .one")));

		var axioms = IMap //
				.<String, Node> empty() //
				.put("@complement", Suite.parse(".P, not .P => false")) //
				.put("@and-lhs", Suite.parse(".P, .Q => .P")) //
				.put("@and-rhs", Suite.parse(".P, .Q => .Q")) //
				.put("@or-lhs", Suite.parse(".P => .P; .Q")) //
				.put("@or-rhs", Suite.parse(".Q => .P; .Q")) //
				.put("@nat-peano1", Suite.parse("true => is-nat 0")) //
				.put("@nat-peano234", Suite.parse("eq-class nat-eq")) //
				.put("@nat-peano5", Suite.parse("is-nat .N, .M nat-eq .N => is-nat .M")) //
				.put("@nat-peano6", Suite.parse("is-nat .N => is-nat (succ .N)")) //
				.put("@nat-peano7", Suite.parse("(is-nat .M, is-nat .N) iff (is-nat (succ .M), is-nat (succ .N))")) //
				.put("@nat-peano8", Suite.parse("succ .N nat-eq 0 => false")) //
				.put("@nat-group0", Suite.parse("group0 is-nat nat-eq nat-add 0")) //
				.put("@nat-add", Suite.parse("is-nat .N => .M nat-eq (succ .N) = succ (.M nat-add .N)")) //
				.put("@set-class", Suite.parse(".E set-in (set-class .C) iff (.C .E)")) //
				.put("@set-union", Suite.parse("(.E set-in (.S set-union .T)) iff (.E set-in .S; .E set-in .T)")) //
				.put("@set-intersect", Suite.parse("(.E set-in (.S set-intersect .T)) iff (.E set-in .S, .E set-in .T)")) //
				.put("@int-group", Suite.parse("true => group is-int int-eq int-add int-neg I0"));

		new Verify(defs, axioms) //
				.extend("suppose @EqClass-Eq := eq-class Eq >> " //
						+ "suppose @P-Eq-Q := .P Eq .Q >> " //
						+ "suppose @Q-Ne-R := not (.Q Eq .R) >> " //
						+ "contradict @P-Eq-R := .P Eq .R >> " //
						+ "lemma @eq := @EqClass-Eq | expand def$eq >> " //
						+ "lemma @Q-Eq-P := @eq | choose symmetric | fulfill-by @P-Eq-Q >> " //
						+ "lemma @Q-Eq-R := @eq | choose transitive | fulfill-by (@Q-Eq-P, @P-Eq-R) >> " //
						+ "@Q-Eq-R, @Q-Ne-R | fulfill @complement") //
				.extend("is-nat 0", "axiom @nat-peano1") //
				.extend("is-nat (succ 0)", "'is-nat 0' | fulfill @nat-peano6");
	}

	private class Definition {
		private Node t0;
		private Node t1;

		private Definition(Node t0, Node t1) {
			this.t0 = t0;
			this.t1 = t1;
		}

		private Definition clone_() {
			var cloner = new Cloner();
			return new Definition(cloner.clone(t0), cloner.clone(t1));
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
			if ((m = Suite.pattern(".0, .1").match(proof)) != null)
				return TreeAnd.of(verify(m[0]), verify(m[1]));
			else if ((m = Suite.pattern(".0 # .1").match(proof)) != null)
				return Tree.of(TermOp.NEXT__, m[0], verify(m[1]));
			else if ((m = Suite.pattern("axiom .0").match(proof)) != null)
				return verify(Suite.substitute("true | fulfill .0", m));
			else if ((m = Suite.pattern(".0 | choose {.1}").match(proof)) != null) {
				var list = verify(m[0]);
				for (var node : Tree.iter(list, TermOp.AND___))
					if (Binder.bind(node, new Generalizer().generalize(m[1]), new Trail()))
						return node;
				return Fail.t("cannot verify " + proof);
			} else if ((m = Suite.pattern(".0 | choose .1").match(proof)) != null) {
				var list = verify(m[0]);
				Tree tree;
				for (var node : Tree.iter(list, TermOp.AND___))
					if ((tree = Tree.decompose(node, TermOp.NEXT__)) != null && tree.getLeft() == m[1])
						return tree.getRight();
				return Fail.t("cannot verify " + proof);
			} else if ((m = Suite.pattern("contradict .0 := .1 >> .2").match(proof)) != null)
				if (Binder.bind(new Verify(defs, rules.put(Atom.name(m[0]), m[1])).verify(m[2]), Atom.FALSE, new Trail()))
					return Suite.substitute("not .0", m[1]);
				else
					return Fail.t("cannot verify " + proof);
			else if ((m = Suite.pattern(".0 | expand .1").match(proof)) != null) {
				var def = defs.get(Atom.name(m[1])).clone_();
				return replace(verify(m[0]), def.t0, def.t1);
			} else if ((m = Suite.pattern(".0 | fulfill .1").match(proof)) != null)
				if ((m1 = Suite.pattern(".0 => .1").match(new Generalizer().generalize(verify(m[1])))) != null
						&& Binder.bind(verify(m[0]), m1[0], new Trail()))
					return m1[1];
				else
					return Fail.t("cannot verify " + proof);
			else if ((m = Suite.pattern(".0 | fulfill-by .1").match(proof)) != null)
				return verify(Suite.substitute(".0 | fulfill .1", m[1], m[0]));
			else if ((m = Suite.pattern("lemma .0 := .1 >> .2").match(proof)) != null)
				return new Verify(defs, rules.put(Atom.name(m[0]), verify(m[1]))).verify(m[2]);
			else if ((m = Suite.pattern(".0 | nat.mi .1 .2").match(proof)) != null) {
				var m_ = m;
				Fun<Node, Node> fun = value -> {
					var generalizer = new Generalizer();
					Binder.bind(generalizer.generalize(m_[1]), value, new Trail());
					return generalizer.generalize(m_[2]);
				};
				var t = Atom.temp();
				var init = fun.apply(Suite.parse("0"));
				var succ = Suite.substitute(".0 => .1", t, fun.apply(Suite.substitute("succ .0", t)));
				Binder.bind(verify(m[0]), TreeAnd.of(init, succ), new Trail());
				return Suite.substitute("is.nat .N => .0", fun.apply(Suite.parse(".N")));
			} else if ((m = Suite.pattern(".0 | rexpand .1").match(proof)) != null) {
				var def = defs.get(Atom.name(m[1])).clone_();
				return replace(verify(m[0]), def.t1, def.t0);
			} else if ((m = Suite.pattern("suppose .0 := .1 >> .2").match(proof)) != null)
				return Suite.substitute(".0 => .1", m[1], new Verify(defs, rules.put(Atom.name(m[0]), m[1])).verify(m[2]));
			else if ((m = Suite.pattern("true").match(proof)) != null)
				return Atom.TRUE;
			else if (proof instanceof Atom)
				return new Cloner().clone(rules.get(Atom.name(proof)));
			else
				return Fail.t("cannot verify " + proof);
		}

		public Verify extend(String lemma, String proof) {
			var node = extend_(proof);
			if (Binder.bind(Suite.parse(lemma), node, new Trail()))
				return new Verify(defs, rules.put(lemma, node));
			else
				return Fail.t();
		}

		public Verify extend(String proof) {
			var node = extend_(proof);
			return new Verify(defs, rules.put(Formatter.dump(node), node));
		}

		private Node extend_(String proof) {
			var node = verify(Suite.parse(proof));
			LogUtil.info("proven :: " + node);
			return node;
		}
	}

	private Node replace(Node node, Node from, Node to) {
		return new Object() {
			private Node replace(Node node_) {
				var generalizer = new Generalizer();
				var t0 = generalizer.generalize(from);
				var t1 = generalizer.generalize(to);
				var trail = new Trail();

				if (Binder.bind(node_, t0, trail))
					return t1;
				else
					trail.unwindAll();

				var tree = Tree.decompose(node_);

				return tree != null //
						? Tree.of(tree.getOperator(), replace(tree.getLeft()), replace(tree.getRight())) //
						: node_;
			}
		}.replace(node);
	}

}
