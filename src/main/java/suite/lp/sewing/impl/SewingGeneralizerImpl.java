package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.HashMap;

import primal.MoreVerbs.Read;
import primal.Verbs.New;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.BaseOp;

public class SewingGeneralizerImpl implements GeneralizerFactory {

	private String variablePrefix;
	private Node cut;
	private VariableMapper<Atom> vm = new VariableMapper<>();

	public static Node generalize(Node node) {
		return new SewingGeneralizerImpl(ProverConstant.variablePrefix).g(node).g().node;
	}

	public SewingGeneralizerImpl(String variablePrefix, Node cut) {
		this(variablePrefix);
		this.cut = cut;
	}

	public SewingGeneralizerImpl(String variablePrefix) {
		this.variablePrefix = variablePrefix;
	}

	@Override
	public VariableMapper<Atom> mapper() {
		return vm;
	}

	@Override
	public Generalize_ generalizer(Node node) {
		var funs = new ArrayList<Generalize_>();
		Generalize_ fun;

		while (true) {
			var node0 = node;
			Tree tree;

			if (node0 instanceof Atom atom) {
				var name = atom.name;
				if (node0 == cut || name.startsWith(variablePrefix)) {
					var index = vm.computeIndex(atom);
					fun = env -> env.get(index);
				} else if (ProverConstant.isWildcard(name))
					fun = env -> new Reference();
				else
					fun = env -> node0;
			} else if (node0 instanceof Dict dict) {
				var array = Read //
						.from2(dict.getMap()) //
						.map((key, value) -> new Generalize_[] { generalizer(key), generalizer(value), }) //
						.toArray(Generalize_[].class);
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
					var f = generalizer(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					var lf = generalizer(tree.getLeft());
					var rf = generalizer(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (node0 instanceof Tuple tuple) {
				var fs = Read.from(tuple.nodes).map(this::generalizer).toArray(Generalize_.class);
				var length = fs.length;
				fun = env -> Tuple.of(New.array(length, Node.class, i -> fs[i].apply(env)));
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
