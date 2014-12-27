package suite.lp.sewing;

import java.util.ArrayList;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;

public class SewingGeneralizer extends VariableMapping {

	public static String wildcardPrefix = "_";
	public static String variablePrefix = ".";
	public static Atom cut = Atom.of("!");

	public static Node generalize(Node node) {
		return process(node).node;
	}

	public static Generalization process(Node node) {
		SewingGeneralizer sg = new SewingGeneralizer();
		Fun<Env, Node> fun = sg.compile(node);
		Env env = sg.env();
		return sg.new Generalization(fun.apply(env), env);
	}

	public Fun<Env, Node> compile(Node node) {
		List<Fun<Env, Node>> funs = new ArrayList<>();
		Fun<Env, Node> fun;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom) {
				String name = ((Atom) node0).name;
				if (isWildcard(name))
					fun = env -> new Reference();
				else if (isVariable(name) || isCut(node0)) {
					int index = findVariableIndex(node0);
					fun = env -> env.get(index);
				} else
					fun = env -> node0;
			} else if ((tree = Tree.decompose(node0)) != null) {
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

	/**
	 * Would a certain end-node be generalized?
	 */
	public static boolean isVariant(Node node) {
		node = node.finalNode();
		if (node instanceof Atom) {
			String name = ((Atom) node).name;
			return isWildcard(name) || isVariable(name) || isCut(node);
		} else
			return false;
	}

	public static boolean isWildcard(String name) {
		return name.startsWith(wildcardPrefix);
	}

	public static boolean isVariable(String name) {
		return name.startsWith(variablePrefix);
	}

	public static boolean isCut(Node node) {
		return node == cut;
	}

}
