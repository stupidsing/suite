package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.HashMap;

import primal.MoreVerbs.Read;
import primal.Verbs.New;
import suite.lp.doer.ClonerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.BaseOp;

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
				var array = Read //
						.from2(Dict.m(node0)) //
						.map((key, value) -> new Clone_[] { cloner(key), cloner(value), }) //
						.toArray(Clone_[].class);
				var length = array.length;
				fun = env -> {
					var map = new HashMap<Node, Reference>();
					for (var i = 0; i < length; i++)
						map.put(array[i][0].apply(env), Reference.of(array[i][1].apply(env)));
					return Dict.of(map);
				};
			} else if ((tree = Tree.decompose(node0)) != null) {
				var operator = tree.getOperator();
				if (operator != BaseOp.OR____) {
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
				var ps = Read.from(Tuple.t(node0)).map(this::cloner).toArray(Clone_.class);
				var size = ps.length;
				fun = env -> Tuple.of(New.array(size, Node.class, i -> ps[i].apply(env)));
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
