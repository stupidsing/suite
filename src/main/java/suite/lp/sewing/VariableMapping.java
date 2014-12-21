package suite.lp.sewing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.util.Pair;
import suite.util.Util;

public class VariableMapping {

	private Map<Node, Integer> variableIndices = new HashMap<>();
	private int nVariables;

	public class Generalization {
		public final Node node;
		private Env env;

		protected Generalization(Node node, Env env) {
			this.node = node;
			this.env = env;
		}

		public String dumpVariables() {
			List<Entry<Node, Node>> entries = Util.sort(getVariables().entrySet(), (e0, e1) -> e0.getKey().compareTo(e1.getKey()));
			StringBuilder sb = new StringBuilder();
			for (Entry<Node, Node> entry : entries) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(Formatter.dump(entry.getKey()) + " = " + Formatter.dump(entry.getValue()));
			}
			return sb.toString();
		}

		public Map<Node, Node> getVariables() {
			return Read.from(variableIndices).toMap(Pair::first_, pair -> env.refs[pair.t1]);
		}

		public Node getVariable(Node variable) {
			return env.refs[variableIndices.get(variable)];
		}
	}

	public static class Env {
		public final Reference refs[];
		public final Node cut;

		private Env(Reference refs[], Node cut) {
			this.refs = refs;
			this.cut = cut;
		}
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

	public int findVariableIndex(Node variable) {
		return variableIndices.computeIfAbsent(variable, any -> nVariables++);
	}

	public Integer getVariableIndex(Node variable) {
		return variableIndices.get(variable);
	}

}
