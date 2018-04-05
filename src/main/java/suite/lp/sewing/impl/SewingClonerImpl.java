package suite.lp.sewing.impl;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.lp.doer.ClonerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
import suite.streamlet.Read;

public class SewingClonerImpl implements ClonerFactory {

	private VariableMapper<Reference> vm = new VariableMapper<>();

	@Override
	public VariableMapper<Reference> mapper() {
		return vm;
	}

	@Override
	public Clone_ cloner(Node node) {
		var funs = new ArrayList<Clone_>();
		Clone_ fun;

		while (true) {
			var node0 = node;
			Tree tree;

			if (node0 instanceof Dict) {
				Clone_[][] array = Read //
						.from2(((Dict) node0).map) //
						.map((key, value) -> new Clone_[] { cloner(key), cloner(value), }) //
						.toArray(Clone_[].class);
				var length = array.length;
				return env -> {
					@SuppressWarnings("unchecked")
					Pair<Node, Reference>[] pairs = new Pair[length];
					for (var i = 0; i < length; i++)
						pairs[i] = Pair.of(array[i][0].apply(env), Reference.of(array[i][1].apply(env)));
					return Dict.of(pairs);
				};
			} else if ((tree = Tree.decompose(node0)) != null) {
				var operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					var f = cloner(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					var lf = cloner(tree.getLeft());
					var rf = cloner(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (node0 instanceof Reference) {
				var index = vm.computeIndex((Reference) node0);
				fun = env -> env.get(index);
			} else if (node0 instanceof Tuple) {
				var ps = Read.from(((Tuple) node0).nodes).map(this::cloner).toArray(Clone_.class);
				var size = ps.length;
				fun = env -> {
					var nodes = new Node[size];
					for (var i = 0; i < size; i++)
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
				var t = Tree.of(null, null, null);
				Node node_ = t;
				for (var fun_ : funs) {
					var t_ = Tree.decompose(node_);
					Tree.forceSetRight(t_, fun_.apply(env));
					node_ = t_.getRight();
				}
				return t.getRight();
			};
		else
			return funs.get(0);
	}

}
