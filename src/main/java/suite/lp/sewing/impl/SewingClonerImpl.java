package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.lp.doer.ClonerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.Read;

public class SewingClonerImpl extends VariableMapper implements ClonerFactory {

	public VariableEnv g(Node node) {
		return g(cloner(node)::apply);
	}

	public Clone_ cloner(Node node) {
		List<Clone_> funs = new ArrayList<>();
		Clone_ fun;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Dict) {
				Clone_[][] array = Read //
						.from2(((Dict) node0).map) //
						.map((key, value) -> new Clone_[] { cloner(key), cloner(value), }) //
						.toArray(Clone_[].class);
				int length = array.length;
				return env -> {
					@SuppressWarnings("unchecked")
					Pair<Node, Reference>[] pairs = new Pair[length];
					for (int i = 0; i < length; i++)
						pairs[i] = Pair.of(array[i][0].apply(env), Reference.of(array[i][1].apply(env)));
					return Dict.of(pairs);
				};
			} else if ((tree = Tree.decompose(node0)) != null) {
				Operator operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					Clone_ f = cloner(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					Clone_ lf = cloner(tree.getLeft());
					Clone_ rf = cloner(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (node0 instanceof Reference) {
				int index = computeIndex(node0);
				fun = env -> env.get(index);
			} else if (node0 instanceof Tuple) {
				Clone_[] ps = Read.from(((Tuple) node0).nodes).map(this::cloner).toArray(Clone_.class);
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
