package suite.math;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.os.LogUtil;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;
import suite.util.To;

public class Symbolic {

	private Int N0 = Int.of(0);
	private Int N1 = Int.of(1);

	public Node d(Node var, Node node0) {
		Rewrite rewrite = new Rewrite(var);
		Node node1 = rewrite.rewrite(node0);
		Node node2 = rewrite.d(node1);
		Node node3;
		try {
			node3 = rewrite.polyize(node2);
		} catch (RuntimeException ex) {
			LogUtil.error(ex);
			node3 = node2;
		}
		return node3;
	}

	private class Rewrite {
		private Node var;

		private Rewrite(Node var) {
			this.var = var;
		}

		// allowed:
		// id(PLUS__) := N0
		// id(MULT__) := N1
		// PLUS__ := Tree.of(PLUS__, a, b)
		// MULT__ := Tree.of(MULT__, a, b)
		// inverse of PLUS__ := Tree.of(MINUS_, N0, a)
		// inverse of MULT__ := Tree.of(DIVIDE, N1, a)
		// exp a
		// ln a
		// sin a
		// i
		private Node rewrite(Node node) {
			Tree tree;
			Node[] m;

			if ((m = Suite.match(".0 - .1").apply(node)) != null && m[0] != N0)
				return Suite.match(".0 + neg .1").substitute(rewrite(m[0]), rewrite(m[1]));
			else if ((m = Suite.match(".0 / .1").apply(node)) != null && m[0] != N1)
				return Suite.match(".0 * inv .1").substitute(rewrite(m[0]), rewrite(m[1]));
			else if (node instanceof Int)
				if (Boolean.FALSE) {
					int n = ((Int) node).number;
					if (n < 0)
						return Suite.match("neg 1 + .0").substitute(rewrite(Int.of(n + 1)));
					else if (n == 0)
						return N0;
					else if (n == 1)
						return N1;
					else
						return Suite.match("1 + .0").substitute(rewrite(Int.of(n - 1)));
				} else
					return node;
			else if ((m = Suite.match(".0 .1").apply(node)) != null)
				return Suite.match(".0 .1").substitute(m[0], rewrite(m[1]));
			else if ((tree = Tree.decompose(node)) != null)
				return Tree.of(tree.getOperator(), rewrite(tree.getLeft()), rewrite(tree.getLeft()));
			else
				return node;
		}

		private Node simplify(Node node0) {
			class Decompose {
				private Operator operator;
				private List<Node> nodes = new ArrayList<>();

				private Decompose(Operator operator) {
					this.operator = operator;
				}

				private void decompose(Node n_) {
					Tree tree = Tree.decompose(n_, operator);
					if (tree != null) {
						decompose(tree.getLeft());
						decompose(tree.getRight());
					} else
						nodes.add(n_);
				}
			}

			Fun2<Operator, Node, List<Node>> decompose = (operator, n_) -> {
				Decompose dec = new Decompose(operator);
				dec.decompose(n_);
				return dec.nodes;
			};

			FixieFun3<Operator, Node, List<Node>, Node> recompose = (operator, init, list) -> {
				if (!list.isEmpty()) {
					Node node = list.get(0);
					for (int i = 1; i < list.size(); i++)
						node = Tree.of(operator, list.get(i), node);
					return node;
				} else
					return init;
			};

			Iterate<Node> decomposeProduct = node -> {
				List<Node> list = new ArrayList<>();
				int xn = 0;
				int constant = 1;

				for (Node child : decompose.apply(TermOp.MULT__, node))
					if (child instanceof Int)
						constant *= ((Int) child).number;
					else if (child == var)
						xn++;
					else
						list.add(child);

				if (constant != 0) {
					for (int i = 0; i < xn; i++)
						list.add(var);
					if (constant != 1)
						list.add(Int.of(constant));

					return recompose.apply(TermOp.MULT__, N1, list);
				} else
					return N0;
			};

			Iterate<Node> decomposeSum = node -> {
				List<Node> list = new ArrayList<>();
				int xn = 0;
				int constant = 0;

				for (Node child0 : decompose.apply(TermOp.PLUS__, node)) {
					Node child1 = decomposeProduct.apply(child0);
					if (child1 instanceof Int)
						constant += ((Int) child1).number;
					else if (child1 == var)
						xn++;
					else
						list.add(child1);
				}

				for (int i = 0; i < xn; i++)
					list.add(var);
				if (constant != 0)
					list.add(Int.of(constant));

				return recompose.apply(TermOp.PLUS__, N0, list);
			};

			return decomposeSum.apply(node0);
		}

		private Node polyize(Node node) { // polynomialize
			class Group implements Fun2<Node, Node, Node> {
				private Operator op;
				private Node e;

				private Group(Operator op, Node e) {
					this.op = op;
					this.e = e;
				}

				public Node apply(Node a, Node b) {
					Tree tree = Tree.of(op, a, b);
					if (a == e)
						return b;
					else if (b == e)
						return a;
					else if (!(a instanceof Int) || !(b instanceof Int))
						return tree;
					else
						return Int.of(TreeUtil.evaluate(tree));
				}
			}

			Fun2<Node, Node, Node> add = new Group(TermOp.PLUS__, N0);
			Fun2<Node, Node, Node> mul = new Group(TermOp.MULT__, N1);

			class Poly {
				private Node[] poly(Node node) { // polynomialize
					Node[] m;

					if ((m = Suite.match(".0 + .1").apply(node)) != null) {
						Node[] ps0 = poly(m[0]);
						Node[] ps1 = poly(m[1]);
						return To.array(Math.max(ps0.length, ps1.length), Node.class, i -> add.apply( //
								i < ps0.length ? ps0[i] : N0, //
								i < ps1.length ? ps1[i] : N0));
					} else if ((m = Suite.match(".0 * .1").apply(node)) != null) {
						Node[] ps0 = poly(m[0]);
						Node[] ps1 = poly(m[1]);
						int length0 = ps0.length;
						int length1 = ps1.length;
						return To.array(length0 + length1 - 1, Node.class, i -> {
							Node sum = N0;
							for (int j = Math.max(0, i - length1 + 1); j <= Math.min(i, length0 - 1); j++)
								sum = add.apply(mul.apply(ps0[j], ps1[i - j]), sum);
							return sum;
						});
					} else if (node == var)
						return new Node[] { N0, N1, };
					else if (node == N0)
						return new Node[] {};
					else if (!isContainsVariable(node))
						return new Node[] { node, };
					else
						throw new RuntimeException();
				}
			}

			Node[] nodes = new Poly().poly(node);
			Node power = N1;
			Node sum = N0;

			for (Node child : nodes) {
				sum = add.apply(mul.apply(child, power), sum);
				power = mul.apply(var, power);
			}

			return sum;
		}

		private Node d(Node node) { // differentiation
			Node[] m;

			if ((m = Suite.match(".0 + .1").apply(node)) != null)
				return Suite.match(".0 + .1").substitute(d(m[0]), d(m[1]));
			else if ((m = Suite.match("neg .0").apply(node)) != null)
				return Suite.match("neg .0").substitute(d(m[0]));
			else if ((m = Suite.match(".0 * .1").apply(node)) != null)
				return Suite.match(".0 * .1 + .2 * .3").substitute(m[0], d(m[1]), m[1], d(m[0]));
			else if ((m = Suite.match("inv .0").apply(node)) != null)
				return Suite.match("neg 1 * inv (.0 * .0) * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("exp .0").apply(node)) != null)
				return Suite.match("exp .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("ln .0").apply(node)) != null)
				return Suite.match("inv .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("sin .0").apply(node)) != null)
				return Suite.match("cos .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("cos .0").apply(node)) != null)
				return Suite.match("neg sin .0 * .1").substitute(m[0], d(m[0]));
			else if (node == var)
				return N1;
			else if (node instanceof Int)
				return N0;
			else
				throw new RuntimeException();
		}

		private boolean isContainsVariable(Node node) {
			Tree tree = Tree.decompose(node);
			return tree != null //
					? isContainsVariable(tree.getLeft()) || isContainsVariable(tree.getRight()) //
					: node == var;
		}
	}

}
