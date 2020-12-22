package suite.math.sym;

import java.util.ArrayList;

import primal.MoreVerbs.Read;
import primal.fp.Funs2.Fun2;
import primal.parser.Operator;
import primal.streamlet.Streamlet;
import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.math.sym.Sym.Field;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.BaseOp;
import suite.node.util.TreeUtil;

public class Express {

	public Int n0 = Int.of(0);
	public Int n1 = Int.of(1);

	public Pattern patAdd = Suite.pattern(".0 + .1");
	public Pattern patNeg = Suite.pattern("neg .0");
	public Pattern patMul = Suite.pattern(".0 * .1");
	public Pattern patInv = Suite.pattern("inv .0");
	public Pattern patPow = Suite.pattern(".0^.1");
	public Pattern patExp = Suite.pattern("exp .0");
	public Pattern patLn_ = Suite.pattern("ln .0");
	public Pattern patSin = Suite.pattern("sin .0");
	public Pattern patCos = Suite.pattern("cos .0");

	public OpGroup add = new OpGroup(null, BaseOp.PLUS__, n0, patNeg);
	public OpGroup mul = new OpGroup(add, BaseOp.MULT__, n1, patInv);

	public Field<Node> field = new Field<>( //
			n0, //
			n1, //
			add::apply, //
			add::inverse, //
			mul::apply, //
			mul::inverse);

	public class OpGroup implements Fun2<Node, Node, Node> {
		private OpGroup group0;
		private Operator operator;
		private Node e;
		private Pattern patInverse;

		private OpGroup(OpGroup group0, Operator operator, Node e, Pattern patInverse) {
			this.group0 = group0;
			this.operator = operator;
			this.e = e;
			this.patInverse = patInverse;
		}

		public Node recompose(Node x, Streamlet<Node> nodes0) {
			var list = new ArrayList<Node>();
			var xn = 0;
			var constant = e;
			Node[] m;
			Node n;

			for (var child : nodes0)
				if (child instanceof Int)
					constant = apply(child, constant);
				else if ((m = patNeg.match(child)) != null && (n = m[0]) instanceof Int)
					constant = apply(Int.of(-Int.num(n)), constant);
				else if (child.compareTo(x) == 0)
					xn++;
				else
					list.add(child);

			for (var i = 0; i < xn; i++)
				list.add(x);

			if (e != constant)
				list.add(intOf(constant));

			var node = e;

			for (var node_ : Read.from(list))
				node = apply(node_, node);

			return node;
		}

		public Node apply(Node a, Node b) {
			var tree = Tree.of(operator, a, b);
			var e0 = group0 != null ? group0.e : null;
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

		public Node identity() {
			return e;
		}

		// TODO for multiplication group, inv inv 0 is NaN
		public Node inverse(Node n) {
			Node[] m;
			if (n == e)
				return e;
			else if ((m = patInverse.match(n)) != null)
				return m[0];
			else
				return patInverse.subst(n);
		}
	}

	public Node intOf(Node n) {
		var i = Int.num(n);
		return i < 0 ? add.inverse(Int.of(-i)) : n;
	}

	public Node intOf(int i) {
		return i < 0 ? add.inverse(Int.of(-i)) : Int.of(i);
	}

}
