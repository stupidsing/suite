package suite.lp.sewing;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Pair;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.NodeWrite;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class SewingCloner extends VariableMapping {

	public static Node generalize(Node node) {
		return process(node).node;
	}

	public static Generalization process(Node node) {
		SewingCloner sc = new SewingCloner();
		Fun<Env, Node> fun = sc.compile(node);
		Env env = sc.env();
		return sc.new Generalization(fun.apply(env), env);
	}

	public Fun<Env, Node> compile(Node node) {
		List<Fun<Env, Node>> funs = new ArrayList<>();
		Fun<Env, Node> fun;
		NodeRead nr;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom)
				fun = env -> node0;
			else if ((tree = Tree.decompose(node0)) != null) {
				Operator operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					Fun<Env, Node> f = compile(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight().finalNode();
					continue;
				} else { // Delay generalizing for performance
					Fun<Env, Node> lf = compile(tree.getLeft());
					Fun<Env, Node> rf = compile(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if ((nr = NodeRead.of(node)).children.size() > 0) {
				List<Pair<Node, Fun<Env, Node>>> ps = Read.from(nr.children) //
						.map(p -> Pair.of(p.t0, compile(p.t1))) //
						.toList();
				fun = env -> {
					List<Pair<Node, Node>> children1 = new ArrayList<>();
					for (Pair<Node, Fun<Env, Node>> child : ps)
						children1.add(Pair.of(child.t0, child.t1.apply(env)));
					return new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
				};
			} else if (node0 instanceof Reference) {
				int index = findVariableIndex(node0);
				fun = env -> env.get(index);
			} else
				fun = env -> node0;

			funs.add(fun);
			break;
		}

		if (funs.size() > 1)
			return env -> {
				Tree t = Tree.of(null, null, null);
				Node node_ = t;
				for (Fun<Env, Node> fun_ : funs) {
					Tree t_ = Tree.decompose(node_);
					Tree.forceSetRight(t_, fun_.apply(env));
					node_ = t_.getRight();
				}
				return t.getRight();
			};
		else
			return funs.get(0);
	}

}
