package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.lp.sewing.Env;
import suite.lp.sewing.SewingGeneralizer;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.NodeWrite;
import suite.node.io.TermOp;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class SewingGeneralizerImpl extends VariableMapperImpl implements SewingGeneralizer {

	public static Node generalize(Node node) {
		return new SewingGeneralizerImpl().g(node).source().node;
	}

	public Source<Generalization> g(Node node) {
		Fun<Env, Node> fun = compile(node);
		return () -> {
			Env env = env();
			return new Generalization(fun.apply(env), env);
		};
	}

	public Fun<Env, Node> compile(Node node) {
		List<Fun<Env, Node>> funs = new ArrayList<>();
		Fun<Env, Node> fun;
		NodeRead nr;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom) {
				String name = ((Atom) node0).name;
				if (VariableMapper.isCut(node0) || VariableMapper.isVariable(name)) {
					int index = findVariableIndex(node0);
					fun = env -> env.get(index);
				} else if (VariableMapper.isWildcard(name))
					fun = env -> new Reference();
				else
					fun = env -> node0;
			} else if ((tree = Tree.decompose(node0)) != null) {
				Operator operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					Fun<Env, Node> f = compile(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					Fun<Env, Node> lf = compile(tree.getLeft());
					Fun<Env, Node> rf = compile(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (0 < (nr = NodeRead.of(node)).children.size()) {
				Streamlet<Pair<Node, Fun<Env, Node>>> ps = Read //
						.from(nr.children) //
						.map(Pair.map1(this::compile)) //
						.collect(As::streamlet);
				fun = env -> ps //
						.map(Pair.map1(f -> f.apply(env))) //
						.collect(outlet -> new NodeWrite(nr.type, nr.terminal, nr.op, outlet.toList()).node);
			} else
				fun = env -> node0;

			funs.add(fun);
			break;
		}

		if (1 < funs.size())
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
