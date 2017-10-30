package suite.math.symbolic;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil2.Fun2;
import suite.util.To;

@SuppressWarnings("unused")
public class Symbolic {

	private Int N0 = Int.of(0);
	private Int N1 = Int.of(1);

	public Node rewrite(Node var, Node node) {
		return new Rewrite(var).rewrite(node);
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
				return Suite.match(".0 + (0 - .1)").substitute(rewrite(m[0]), rewrite(m[1]));
			else if ((m = Suite.match(".0 / .1").apply(node)) != null && m[0] != N1)
				return Suite.match(".0 * (1 / .1)").substitute(rewrite(m[0]), rewrite(m[1]));
			else if (node instanceof Int)
				if (Boolean.FALSE) {
					int n = ((Int) node).number;
					if (n < 0)
						return Suite.match("(0 - 1) + .0").substitute(rewrite(Int.of(n + 1)));
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

			class DecomposeProduct {
				private List<Node> list = new ArrayList<>();
				private int xn = 0;
				private int constant = 1;

				private Node decompose(Node n0) {
					for (Node child : decompose.apply(TermOp.MULT__, n0))
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

						Node n1;

						if (!list.isEmpty()) {
							n1 = list.get(0);
							for (int i = 1; i < list.size(); i++)
								n1 = Suite.match(".0 * .1").substitute(n1, list.get(i));
						} else
							n1 = N1;

						return n1;
					} else
						return N0;
				}
			}

			class DecomposeSum {
				private List<Node> list = new ArrayList<>();
				private int xn = 0;
				private int constant = 0;

				private Node decompose(Node n0) {
					for (Node child0 : decompose.apply(TermOp.PLUS__, n0)) {
						DecomposeProduct dec = new DecomposeProduct();
						Node child1 = dec.decompose(child0);
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

					Node n1;

					if (!list.isEmpty()) {
						n1 = list.get(0);
						for (int i = 1; i < list.size(); i++)
							n1 = Suite.match(".0 + .1").substitute(n1, list.get(i));
					} else
						n1 = N1;

					return n1;
				}
			}

			Tree tree = Tree.decompose(node0);
			Node node1 = tree != null ? Tree.of(tree.getOperator(), simplify(tree.getLeft()), simplify(tree.getRight())) : node0;
			Node node2 = node1;
			return node2;
		}

		private Streamlet<Node> sumOfProds(Node node) {
			Node[] m;

			if ((m = Suite.match("ln (.0 * .1)").apply(node)) != null)
				return sumOfProds(Suite.match("ln .0 + ln .1").substitute(m));
			else if ((m = Suite.match(".0 * (.1 + .2)").apply(node)) != null //
					|| (m = Suite.match("(.1 + .2) * .0").apply(node)) != null)
				return sumOfProds(Suite.match(".0 * .1 + .0 * .2").substitute(m));
			else if ((m = Suite.match(".0 + .1").apply(node)) != null)
				return Streamlet.concat(sumOfProds(m[0]), sumOfProds(m[1]));
			else if (node == N0)
				return Read.empty();
			else
				return Read.each(node);
		}

		private Streamlet<Node> prodOfPowers(Node node) {
			Node[] m;

			if ((m = Suite.match("exp (.0 + .1)").apply(node)) != null)
				return prodOfPowers(Suite.match("exp .0 * exp .1").substitute(m));
			else if ((m = Suite.match(".0 * .1").apply(node)) != null)
				return Streamlet.concat(prodOfPowers(m[0]), prodOfPowers(m[1]));
			else if (node == N1)
				return Read.empty();
			else
				return Read.each(node);
		}

		private Node powers(Node node) {
			Node[] m;

			if (node == var || node instanceof Int)
				return node;
			else if ((m = Suite.match("exp .0").apply(node)) != null)
				return Suite.match("exp .0").substitute(powers(m[0]));
			else
				throw new RuntimeException();
		}

		private Node[] polyize(Node node) {
			Node[] m;

			if ((m = Suite.match(".0 + .1").apply(node)) != null) {
				Node[] ps0 = polyize(m[0]);
				Node[] ps1 = polyize(m[1]);
				return To.array(Math.max(ps0.length, ps1.length), Node.class, i -> Suite.match(".0 + .1").substitute( //
						i < ps0.length ? ps0[i] : N0, //
						i < ps1.length ? ps1[i] : N0));
			} else if ((m = Suite.match(".0 * .1").apply(node)) != null) {
				Node[] ps0 = polyize(m[0]);
				Node[] ps1 = polyize(m[1]);
				int length0 = ps0.length;
				int length1 = ps1.length;
				return To.array(length0 + length1 - 1, Node.class, i -> {
					Node sum = N0;
					for (int j = Math.max(0, i - length1 + 1); j <= Math.min(i, length0 - 1); j++)
						sum = Suite.match(".0 * .1 + .2").substitute(ps0[j], ps1[i - j], sum);
					return sum;
				});
			} else if (node == var)
				return new Node[] { N0, N1, };
			else if (node == N0)
				return new Node[] {};
			else if (node instanceof Int)
				return new Node[] { node, };
			else
				throw new RuntimeException();
		}

		private Node d(Node node) {
			Node[] m;

			if ((m = Suite.match(".0 + .1").apply(node)) != null)
				return Suite.match(".0 + .1").substitute(d(m[0]), d(m[1]));
			else if ((m = Suite.match("0 - .0").apply(node)) != null)
				return Suite.match("0 - .0").substitute(d(m[0]));
			else if ((m = Suite.match(".0 * .1").apply(node)) != null)
				return Suite.match(".0 * .1 + .2 * .3").substitute(m[0], d(m[1]), m[1], d(m[0]));
			else if ((m = Suite.match("1 / .0").apply(node)) != null)
				return Suite.match("(0 - 1) * 1 / .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("exp .0").apply(node)) != null)
				return Suite.match("exp .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = Suite.match("ln .0").apply(node)) != null)
				return Suite.match("1 / .0 * .1").substitute(m[0], d(m[0]));
			else if (node == var)
				return N1;
			else if (node instanceof Int)
				return N0;
			else
				throw new RuntimeException();
		}
	}

}
