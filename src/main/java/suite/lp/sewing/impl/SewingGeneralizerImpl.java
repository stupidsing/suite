package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
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
import suite.node.io.TermOp;
import suite.streamlet.Read;

public class SewingGeneralizerImpl implements GeneralizerFactory {

	private VariableMapper<Atom> vm = new VariableMapper<>();

	public static Node generalize(Node node) {
		return new SewingGeneralizerImpl().g(node).source().node;
	}

	@Override
	public VariableMapper<Atom> mapper() {
		return vm;
	}

	@Override
	public Generalize_ generalizer(Node node) {
		List<Generalize_> funs = new ArrayList<>();
		Generalize_ fun;

		while (true) {
			var node0 = node;
			Tree tree;

			if (node0 instanceof Atom) {
				var atom = (Atom) node0;
				var name = atom.name;
				if (ProverConstant.isCut(node0) || ProverConstant.isVariable(name)) {
					var index = vm.computeIndex(atom);
					fun = env -> env.get(index);
				} else if (ProverConstant.isWildcard(name))
					fun = env -> new Reference();
				else
					fun = env -> node0;
			} else if (node0 instanceof Dict) {
				Generalize_[][] array = Read //
						.from2(((Dict) node0).map) //
						.map((key, value) -> new Generalize_[] { generalizer(key), generalizer(value), }) //
						.toArray(Generalize_[].class);
				var length = array.length;
				fun = env -> {
					@SuppressWarnings("unchecked")
					Pair<Node, Reference>[] pairs = new Pair[length];
					for (var i = 0; i < length; i++)
						pairs[i] = Pair.of(array[i][0].apply(env), Reference.of(array[i][1].apply(env)));
					return Dict.of(pairs);
				};
			} else if ((tree = Tree.decompose(node0)) != null) {
				var operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					var f = generalizer(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					var lf = generalizer(tree.getLeft());
					var rf = generalizer(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (node0 instanceof Tuple) {
				Generalize_[] fs = Read.from(((Tuple) node0).nodes).map(this::generalizer).toArray(Generalize_.class);
				var length = fs.length;
				fun = env -> {
					var array = new Node[length];
					for (var i = 0; i < length; i++)
						array[i] = fs[i].apply(env);
					return Tuple.of(array);
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
				for (Generalize_ fun_ : funs) {
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
