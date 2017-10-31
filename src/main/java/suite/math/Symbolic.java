package suite.math;

import java.util.ArrayList;
import java.util.List;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil2.Fun2;
import suite.util.To;

public class Symbolic {

	private Int N0 = Int.of(0);
	private Int N1 = Int.of(1);

	public class PolynomializeException extends RuntimeException {
		private static final long serialVersionUID = 1l;
	}

	public Node d(Node var, Node node0) {
		Rewrite rewrite = new Rewrite(var);
		Node node1 = rewrite.rewrite(node0);
		Node node2 = rewrite.d(node1);
		Node node3;
		try {
			node3 = rewrite.polyize(node2);
		} catch (PolynomializeException ex) {
			node3 = rewrite.sumOfProducts(node2);
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

		private Node sumOfProducts(Node node) {
			class Recurse {
				private Streamlet<Node> pos(Node node_) {
					Node[] m;
					if ((m = matchMul.apply(node_)) != null)
						return Streamlet.concat(pos(m[0]), pos(m[1]));
					else if ((m = matchInv.apply(node_)) != null)
						return pos(m[0]).map(matchInv::substitute);
					else if ((m = matchPow.apply(node_)) != null)
						return pos(m[0]).join2(sop(m[1])).map(matchPow::substitute);
					else if ((m = matchExp.apply(node_)) != null)
						return sop(m[0]).map(matchExp::substitute);
					else if (node_ instanceof Tree)
						return Read.each(sumOfProduct(node_));
					else
						return Read.each(node_);
				}

				private Streamlet<Node> sop(Node node_) {
					Node[] m;
					if ((m = matchAdd.apply(node_)) != null)
						return Streamlet.concat(sop(m[0]), sop(m[1]));
					else if ((m = matchNeg.apply(node_)) != null)
						return sop(m[0]).map(matchNeg::substitute);
					else if ((m = matchMul.apply(node_)) != null)
						return sop(m[0]).join2(sop(m[1])).map(mul::apply);
					else if ((m = matchLn.apply(node_)) != null)
						return pos(m[0]).map(matchLn::substitute);
					else if (node_ instanceof Tree)
						return Read.each(productOfSum(node_));
					else
						return Read.each(node_);
				}

				private Node productOfSum(Node node) {
					Streamlet<Node> pos = pos(node);
					if (Boolean.FALSE)
						return mul.recompose(pos);
					else
						return mul.recompose(var, pos);
				}

				private Node sumOfProduct(Node node) {
					Streamlet<Node> sop = sop(node);
					if (Boolean.FALSE)
						return add.recompose(sop);
					else
						return add.recompose(var, sop);
				}
			}

			return new Recurse().sumOfProduct(node);
		}

		private Node polyize(Node node) { // polynomialize
			class Poly {
				private Node[] poly(Node node) {
					Node[] m;
					if ((m = matchAdd.apply(node)) != null) {
						Node[] ps0 = poly(m[0]);
						Node[] ps1 = poly(m[1]);
						return To.array(Math.max(ps0.length, ps1.length), Node.class, i -> add.apply( //
								i < ps0.length ? ps0[i] : N0, //
								i < ps1.length ? ps1[i] : N0));
					} else if ((m = matchMul.apply(node)) != null) {
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
						throw new PolynomializeException();
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
			if ((m = matchAdd.apply(node)) != null)
				return matchAdd.substitute(d(m[0]), d(m[1]));
			else if ((m = matchNeg.apply(node)) != null)
				return matchNeg.substitute(d(m[0]));
			else if ((m = matchMul.apply(node)) != null)
				return Suite.match(".0 * .1 + .2 * .3").substitute(m[0], d(m[1]), m[1], d(m[0]));
			else if ((m = matchInv.apply(node)) != null)
				return Suite.match("neg 1 * inv (.0 * .0) * .1").substitute(m[0], d(m[0]));
			else if ((m = matchExp.apply(node)) != null)
				return Suite.match("exp .0 * .1").substitute(m[0], d(m[0]));
			else if ((m = matchLn.apply(node)) != null)
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

	private Group add = new Group(TermOp.PLUS__, N0);
	private Group mul = new Group(TermOp.MULT__, N1);

	private class Group implements Fun2<Node, Node, Node> {
		private Operator op;
		private Node e;

		private Group(Operator op, Node e) {
			this.op = op;
			this.e = e;
		}

		private Node recompose(Node var, Streamlet<Node> nodes) {
			List<Node> list = new ArrayList<>();
			int xn = 0;
			Node constant = e;

			for (Node child : nodes)
				if (child instanceof Int)
					constant = apply(child, constant);
				else if (child == var)
					xn++;
				else
					list.add(child);

			for (int i = 0; i < xn; i++)
				list.add(var);

			if (e != constant)
				list.add(constant);

			return recompose(Read.from(list));
		}

		private Node recompose(Streamlet<Node> nodes) {
			Node node = nodes.first();
			if (node != null)
				for (Node node1 : nodes.drop(1))
					node = apply(node1, node);
			else
				node = e;
			return node;
		}

		public Node apply(Node a, Node b) {
			Tree tree = Tree.of(op, a, b);
			if (a == e)
				return b;
			else if (b == e)
				return a;
			else if (a instanceof Int && b instanceof Int)
				return Int.of(TreeUtil.evaluate(tree));
			else
				return tree;
		}
	}

	private Match matchAdd = Suite.match(".0 + .1");
	private Match matchNeg = Suite.match("neg .0");
	private Match matchMul = Suite.match(".0 * .1");
	private Match matchInv = Suite.match("inv .0");
	private Match matchPow = Suite.match(".0^.1");
	private Match matchExp = Suite.match("exp .0");
	private Match matchLn = Suite.match("ln .0");

}
