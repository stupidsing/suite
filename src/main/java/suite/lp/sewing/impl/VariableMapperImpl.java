package suite.lp.sewing.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.IdentityKey;
import suite.lp.sewing.VariableMapper;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.util.Util;

public class VariableMapperImpl implements VariableMapper {

	private Map<IdentityKey<Node>, Integer> variableIndices = new HashMap<>();
	private int nVariables;

	public class Generalization {
		public final Node node;
		private Env env;

		public Generalization(Node node, Env env) {
			this.node = node;
			this.env = env;
		}

		public String dumpVariables() {
			List<Entry<Node, Node>> entries = Util.sort(getVariables().entrySet(), (e0, e1) -> e0.getKey().compareTo(e1.getKey()));
			StringBuilder sb = new StringBuilder();
			for (Entry<Node, Node> entry : entries) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(Formatter.dump(entry.getKey()) + " = " + Formatter.dump(entry.getValue().finalNode()));
			}
			return sb.toString();
		}

		public Map<Node, Node> getVariables() {
			return Read.from(variableIndices).toMap(pair -> pair.t0.key, pair -> env.refs[pair.t1]);
		}

		public Node getVariable(Node variable) {
			return env.refs[variableIndices.get(IdentityKey.of(variable))];
		}
	}

	public Env env() {
		Reference refs[] = new Reference[nVariables];
		for (int i = 0; i < nVariables; i++)
			refs[i] = new Reference();
		return new Env(refs);
	}

	public int findVariableIndex(Node variable) {
		return variableIndices.computeIfAbsent(IdentityKey.of(variable), any -> nVariables++);
	}

	public Integer getVariableIndex(Node variable) {
		return variableIndices.get(IdentityKey.of(variable));
	}

}
