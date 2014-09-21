package suite.lp.sewing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class SewingGeneralizer {

	public static String wildcardPrefix = "_";
	public static String variablePrefix = ".";
	public static String cutName = "!";

	private Map<Node, Integer> variableIndices = new HashMap<>();
	private int nVariables;

	public class Generalization {
		private Node node;
		private Env env;

		private Generalization(Node node, Env env) {
			this.node = node;
			this.env = env;
		}

		public Node getVariable(Node variable) {
			return env.refs[variableIndices.get(variable)];
		}

		public String dumpVariables() {
			return SewingGeneralizer.this.dumpVariables(env);
		}

		public Node node() {
			return node;
		}
	}

	public static class Env {
		private Reference refs[];
		private Node cut;

		private Env(Reference refs[], Node cut) {
			this.refs = refs;
			this.cut = cut;
		}
	}

	public static Node generalize(Node node) {
		return process(node).node;
	}

	public static Generalization process(Node node) {
		SewingGeneralizer sg = new SewingGeneralizer();
		Fun<Env, Node> fun = sg.compile(node);
		Env env = sg.env();
		return sg.new Generalization(fun.apply(env), env);
	}

	public Env env() {
		return env(null);
	}

	public Env env(Node cut) {
		Reference refs[] = new Reference[nVariables];
		for (int i = 0; i < nVariables; i++)
			refs[i] = new Reference();
		return new Env(refs, cut);
	}

	public Fun<Env, Node> compile(Node node) {
		List<Fun<Env, Node>> funs = new ArrayList<>();
		Fun<Env, Node> fun;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom) {
				String name = ((Atom) node0).getName();

				if (isWildcard(name))
					fun = env -> new Reference();
				else if (isVariable(name)) {
					int index = getVariableIndex(node0);
					fun = env -> env.refs[index];
				} else if (isCut(name))
					fun = env -> env.cut;
				else
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
				for (Fun<Env, Node> gen_ : funs) {
					Tree t_ = Tree.decompose(node_);
					Tree.forceSetRight(t_, gen_.apply(env));
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
			String name = ((Atom) node).getName();
			return isWildcard(name) || isVariable(name) || isCut(name);
		} else
			return false;
	}

	private Integer getVariableIndex(Node variable) {
		return variableIndices.computeIfAbsent(variable, any -> nVariables++);
	}

	private String dumpVariables(Env env) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		List<Entry<Node, Integer>> entries = Util.sort(variableIndices.entrySet(), (e0, e1) -> e0.getKey().compareTo(e1.getKey()));

		for (Entry<Node, Integer> entry : entries) {
			if (first)
				first = false;
			else
				sb.append(", ");

			sb.append(Formatter.dump(entry.getKey()));
			sb.append(" = ");
			sb.append(Formatter.dump(env.refs[entry.getValue()]));
		}

		return sb.toString();
	}

	private static boolean isWildcard(String name) {
		return name.startsWith(wildcardPrefix);
	}

	private static boolean isVariable(String name) {
		return name.startsWith(variablePrefix);
	}

	private static boolean isCut(String name) {
		return name.equals(cutName);
	}

}
