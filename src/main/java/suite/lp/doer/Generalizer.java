package suite.lp.doer;

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

public class Generalizer {

	private static String wildcardPrefix = "_";
	public static String variablePrefix = ".";
	private static String cutName = "!";

	private Map<Node, Integer> variableIndices = new HashMap<>();
	private int nVariables;

	public interface Producer {
		public Node produce(Env env);
	}

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
			return Generalizer.this.dumpVariables(env);
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
		Generalizer generalizer = new Generalizer();
		Fun<Env, Node> fun = generalizer.compile(node);
		Env env = generalizer.env();
		return generalizer.new Generalization(fun.apply(env), env);
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
		return compileRight(Tree.of(null, null, node))::produce;
	}

	private Producer compileRight(Tree tree) {
		List<Producer> gens = new ArrayList<>();
		Producer gen;

		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight().finalNode();
			Tree rt;

			if (right instanceof Atom) {
				String name = ((Atom) right).getName();

				if (isWildcard(name))
					gen = env -> new Reference();
				else if (isVariable(name)) {
					int index = getVariableIndex(right);
					gen = env -> env.refs[index];
				} else if (isCut(name))
					gen = env -> env.cut;
				else
					gen = env -> right;
			} else if ((rt = Tree.decompose(right)) != null) {
				Operator operator = tree.getOperator();

				if (operator != TermOp.OR____) {
					Fun<Env, Node> fun = compile(rt.getLeft());
					gen = env -> Tree.of(rt.getOperator(), fun.apply(env), null);
					nextTree = rt;
				} else { // Delay generalizing for performance
					Fun<Env, Node> fun = compile(rt);
					gen = env -> new Suspend(() -> fun.apply(env));
				}
			} else
				gen = env -> right;

			gens.add(gen);
			tree = nextTree;
		}

		if (gens.size() > 1)
			return env -> {
				Tree t = Tree.of(null, null, null);
				Node node = t;
				for (Producer gen_ : gens) {
					Tree t_ = Tree.decompose(node);
					Tree.forceSetRight(t_, gen_.produce(env));
					node = t_.getRight();
				}
				return t.getRight();
			};
		else
			return gens.get(0);
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
