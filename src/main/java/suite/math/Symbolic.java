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
		// inverse of PLUS__ := neg a
		// inverse of MULT__ := inv a
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
			else if ((m = matchPow.apply(node)) != null)
				return Suite.match("exp (ln .0 * .1)").substitute(m[0], m[1]);
			else if (node instanceof Int)
				return intOf((Int) node);
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
						return Read.each(sumOfProducts(node_));
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
						return sop(m[0]).join2(sop(m[1])).map(mul::apply).map(this::productOfSums);
					else if ((m = matchLn.apply(node_)) != null)
						return pos(m[0]).map(matchLn::substitute);
					else if ((m = Suite.match("sin (.0 + .1)").apply(node_)) != null)
						return Read.each( //
								Suite.substitute("sin .0 * cos .1", m), //
								Suite.substitute("cos .0 * sin .1", m)) //
								.map(this::productOfSums);
					else if ((m = Suite.match("cos (.0 + .1)").apply(node_)) != null)
						return Read.each( //
								Suite.substitute("cos .0 * cos .1", m), //
								Suite.substitute("neg sin .0 * sin .1", m)) //
								.map(this::productOfSums);
					else if (node_ instanceof Tree)
						return Read.each(productOfSums(node_));
					else
						return Read.each(node_);
				}

				private Node productOfSums(Node node) {
					return mul.recompose(var, pos(node));
				}

				private Node sumOfProducts(Node node) {
					return add.recompose(var, sop(node));
				}
			}

			return new Recurse().sumOfProducts(node);
		}

		private Node polyize(Node node) { // polynomialize
			class Poly {
				private Node[] poly(Node node) {
					Node[] ps0, ps1;
					Node[] m;
					if ((m = matchAdd.apply(node)) != null) {
						int length0 = (ps0 = poly(m[0])).length;
						int length1 = (ps1 = poly(m[1])).length;
						return To.array(Math.max(length0, length1), Node.class, i -> add.apply( //
								i < length0 ? ps0[i] : N0, //
								i < length1 ? ps1[i] : N0));
					} else if ((m = matchMul.apply(node)) != null) {
						int length0 = (ps0 = poly(m[0])).length;
						int length1 = (ps1 = poly(m[1])).length;
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
				return Suite.match("inv (.0 * .0) * neg .1").substitute(m[0], d(m[0]));
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

	private Group add = new Group(null, TermOp.PLUS__, N0);
	private Group mul = new Group(add, TermOp.MULT__, N1);

	private class Group implements Fun2<Node, Node, Node> {
		private Group group0;
		private Operator operator;
		private Node e;

		private Group(Group group0, Operator operator, Node e) {
			this.group0 = group0;
			this.operator = operator;
			this.e = e;
		}

		private Node recompose(Node var, Streamlet<Node> nodes0) {
			List<Node> list = new ArrayList<>();
			int xn = 0;
			Node constant = e;
			Node[] m;
			Node n;

			for (Node child : nodes0)
				if (child instanceof Int)
					constant = apply(child, constant);
				else if ((m = matchNeg.apply(child)) != null && (n = m[0]) instanceof Int)
					constant = apply(Int.of(-((Int) n).number), constant);
				else if (child == var)
					xn++;
				else
					list.add(child);

			for (int i = 0; i < xn; i++)
				list.add(var);

			if (e != constant)
				list.add(intOf(constant));

			Streamlet<Node> nodes1 = Read.from(list);
			Node node = nodes1.first();

			if (node != null)
				for (Node node1 : nodes1.drop(1))
					node = apply(node1, node);
			else
				node = e;
			return node;
		}

		public Node apply(Node a, Node b) {
			Tree tree = Tree.of(operator, a, b);
			Node e0 = group0 != null ? group0.e : null;
			if (a == e0 || b == e0)
				return e0;
			else if (a == e)
				return b;
			else if (b == e)
				return a;
			else if (a instanceof Int && b instanceof Int)
				return Int.of(TreeUtil.evaluate(tree));
			else
				return tree;
		}
	}

	private Node intOf(Node n) {
		int i = ((Int) n).number;
		return i < 0 ? matchNeg.substitute(Int.of(-i)) : n;
	}

	private Match matchAdd = Suite.match(".0 + .1");
	private Match matchNeg = Suite.match("neg .0");
	private Match matchMul = Suite.match(".0 * .1");
	private Match matchInv = Suite.match("inv .0");
	private Match matchPow = Suite.match(".0^.1");
	private Match matchExp = Suite.match("exp .0");
	private Match matchLn = Suite.match("ln .0");

}
