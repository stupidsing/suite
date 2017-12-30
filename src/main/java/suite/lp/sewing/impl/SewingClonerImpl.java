package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.lp.sewing.Env;
import suite.lp.sewing.SewingCloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.NodeWrite;
import suite.node.io.TermOp;
import suite.streamlet.Read;

public class SewingClonerImpl extends VariableMapperImpl implements SewingCloner {

	public static Node generalize(Node node) {
		return process(node).node;
	}

	public static Generalization process(Node node) {
		SewingClonerImpl sc = new SewingClonerImpl();
		Clone_ fun = sc.compile(node);
		Env env = sc.env();
		return sc.new Generalization(fun.apply(env), env);
	}

	public Clone_ compile(Node node) {
		List<Clone_> funs = new ArrayList<>();
		Clone_ fun;
		NodeRead nr;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom)
				fun = env -> node0;
			else if ((tree = Tree.decompose(node0)) != null) {
				Operator operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					Clone_ f = compile(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					Clone_ lf = compile(tree.getLeft());
					Clone_ rf = compile(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (0 < (nr = NodeRead.of(node)).children.size()) {
				List<Pair<Node, Clone_>> ps = Read.from(nr.children) //
						.map(Pair.map1(this::compile)) //
						.toList();
				fun = env -> {
					List<Pair<Node, Node>> children1 = Read.from(ps) //
							.map(Pair.map1(f -> f.apply(env))) //
							.toList();
					return new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
				};
			} else if (node0 instanceof Reference) {
				int index = findVariableIndex(node0);
				fun = env -> env.get(index);
			} else if (node0 instanceof Tuple) {
				Clone_[] ps = Read.from(((Tuple) node0).nodes).map(this::compile).toArray(Clone_.class);
				int size = ps.length;
				fun = env -> {
					Node[] nodes = new Node[size];
					for (int i = 0; i < size; i++)
						nodes[i] = ps[i].apply(env);
					return Tuple.of(nodes);
				};
			} else
				fun = env -> node0;

			funs.add(fun);
			break;
		}

		if (1 < funs.size())
			return env -> {
				Tree t = Tree.of(null, null, null);
				Node node_ = t;
				for (Clone_ fun_ : funs) {
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
