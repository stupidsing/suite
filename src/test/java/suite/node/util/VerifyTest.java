package suite.node.util;

import static primal.statics.Fail.fail;

import org.junit.Test;

import primal.fp.Funs.Fun;
import primal.fp.Funs2.Fun2;
import primal.os.Log_;
import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.persistent.PerList;
import suite.persistent.PerMap;

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
		var and = new Fun<PerList<String>, Node>() {
			public Node apply(PerList<String> list) {
				return !list.isEmpty() ? Tree.ofAnd(Suite.parse(list.head), apply(list.tail)) : Atom.TRUE;
			}
		};

		Fun2<String, Node, Definition> def_ = (t0, t1) -> new Definition(Suite.parse(t0), t1);
		Fun2<String, String, Definition> def2 = (t0, t1) -> def_.apply(t0, Suite.parse(t1));
		Fun2<String, PerList<String>, Definition> defn = (t0, t1) -> def_.apply(t0, and.apply(t1));

		var defs = PerMap //
				.<String, Definition> empty() //
				.put("def$iff", defn.apply("iff .A .B", PerList.of( //
						"fore # .A => .B", //
						"back # .B => .A"))) //
				.put("def$eq", defn.apply("eq-class .eq", PerList.of( //
						"reflexive # true => .A .eq .A", //
						"symmetric # .A .eq .B => .B .eq .A", //
						"transitive # .A .eq .B, .B .eq .C => .A .eq .C"))) //
				.put("def$uni-op", def2.apply("uni-op .class .op", //
						".class .P => .class (.op .P)")) //
				.put("def$bin-op", def2.apply("bin-op .class .op", //
						".class .P, .class .Q => .class (.P .op .Q)")) //
				.put("def$set", defn.apply("set .eq", PerList.of( //
						".S .eq .T => (.E set-in .S) iff (.E set-in .T)"))) //
				.put("def$group0", defn.apply("group0 .class .eq .op .zero", PerList.of( //
						".class .zero", //
						"eq-class .eq", //
						".class .P, .P .eq .Q => .class .Q", //
						"bin-op .class .op", //
						".class .P => (.zero .op .P) .eq .P", //
						".class .P, .class .Q, .class .R => (.P .op (.Q .op .R)) .eq ((.P .op .Q) .op .R)"))) //
				.put("def$group", defn.apply("group .class .eq .op .inv .zero", PerList.of( //
						"group0 .class .eq .op .zero", //
						".class .P => (.P .op (.inv .P)) .eq .zero"))) //
				.put("def$field", defn.apply("field .class .eq .op0 .inv0 .zero .op1 .inv1 .one", PerList.of( //
						"group .class .eq .op0 .inv0 .zero", //
						"group0 .class .eq .op1 .inv1 .one", //
						".class .P => .P .eq .zero; (.P .op1 (.inv1 .P)) .eq .one")));

		var axioms = PerMap //
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
				.extend("suppose @EqClass-Eq := eq-class Eq ~ " //
						+ "suppose @P-Eq-Q := .P Eq .Q ~ " //
						+ "suppose @Q-Ne-R := not (.Q Eq .R) ~ " //
						+ "contradict @P-Eq-R := .P Eq .R ~ " //
						+ "lemma @eq := @EqClass-Eq | expand def$eq ~ " //
						+ "lemma @Q-Eq-P := @eq | choose symmetric | fulfill-by @P-Eq-Q ~ " //
						+ "lemma @Q-Eq-R := @eq | choose transitive | fulfill-by (@Q-Eq-P, @P-Eq-R) ~ " //
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
		private PerMap<String, Definition> defs;
		private PerMap<String, Node> rules;

		private Verify(PerMap<String, Definition> defs, PerMap<String, Node> rules) {
			this.defs = defs;
			this.rules = rules;
		}

		private Node verify(Node proof) {
			return new SwitchNode<Node>(proof //
			).match(".0, .1", (a, b) -> {
				return Tree.ofAnd(verify(a), verify(b));
			}).match(".0 # .1", (a, b) -> {
				return Tree.of(TermOp.NEXT__, a, verify(b));
			}).match("axiom .0", a -> {
				return verify(Suite.substitute("true | fulfill .0", a));
			}).match(".0 | choose_{.1}", (a, b) -> {
				var list = verify(a);
				for (var node : Tree.read(list))
					if (Binder.bind(node, new Generalizer().generalize(b), new Trail()))
						return node;
				return fail("cannot verify " + proof);
			}).match(".0 | choose .1", (a, b) -> {
				var list = verify(a);
				Tree tree;
				for (var node : Tree.read(list))
					if ((tree = Tree.decompose(node, TermOp.NEXT__)) != null && tree.getLeft() == b)
						return tree.getRight();
				return fail("cannot verify " + proof);
			}).match("contradict .0 := .1 ~ .2", (a, b, c) -> {
				var x = Binder.bind(new Verify(defs, rules.put(Atom.name(a), b)).verify(c), Atom.FALSE, new Trail());
				return x ? Suite.substitute("not .0", b) : fail("cannot verify " + proof);
			}).match(".0 | expand .1", (a, b) -> {
				var def = defs.get(Atom.name(b)).clone_();
				return replace(verify(a), def.t0, def.t1);
			}).match(".0 | fulfill .1", (a, b) -> {
				var m1 = Suite.pattern(".0 => .1").match(new Generalizer().generalize(verify(b)));
				var x = m1 != null && Binder.bind(verify(a), m1[0], new Trail());
				return x ? m1[1] : fail("cannot verify " + proof);
			}).match(".0 | fulfill-by .1", (a, b) -> {
				return verify(Suite.substitute(".0 | fulfill .1", b, a));
			}).match("lemma .0 := .1 ~ .2", (a, b, c) -> {
				return new Verify(defs, rules.put(Atom.name(a), verify(b))).verify(c);
			}).match(".0 | nat.mi .1 .2", (a, b, c) -> {
				Fun<Node, Node> fun = value -> {
					var generalizer = new Generalizer();
					Binder.bind(generalizer.generalize(b), value, new Trail());
					return generalizer.generalize(c);
				};
				var t = Atom.temp();
				var init = fun.apply(Suite.parse("0"));
				var succ = Suite.substitute(".0 => .1", t, fun.apply(Suite.substitute("succ .0", t)));
				Binder.bind(verify(a), Tree.ofAnd(init, succ), new Trail());
				return Suite.substitute("is.nat .N => .0", fun.apply(Suite.parse(".N")));
			}).match(".0 | rexpand .1", (a, b) -> {
				var def = defs.get(Atom.name(b)).clone_();
				return replace(verify(a), def.t1, def.t0);
			}).match("suppose .0 := .1 ~ .2", (a, b, c) -> {
				return Suite.substitute(".0 => .1", b, new Verify(defs, rules.put(Atom.name(a), b)).verify(c));
			}).match("true", () -> {
				return Atom.TRUE;
			}).applyIf(Atom.class, a -> {
				return new Cloner().clone(rules.get(a.name));
			}).nonNullResult();
		}

		public Verify extend(String lemma, String proof) {
			var node = extend_(proof);
			var x = Binder.bind(Suite.parse(lemma), node, new Trail());
			return x ? new Verify(defs, rules.put(lemma, node)) : fail();
		}

		public Verify extend(String proof) {
			var node = extend_(proof);
			return new Verify(defs, rules.put(Formatter.dump(node), node));
		}

		private Node extend_(String proof) {
			var node = verify(Suite.parse(proof));
			Log_.info("proven :: " + node);
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
