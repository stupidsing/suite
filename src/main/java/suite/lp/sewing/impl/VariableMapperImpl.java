package suite.lp.sewing.impl;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.lp.sewing.VariableMapper;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
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
			Streamlet<String> kvs = Read.from(variableIndices) //
					.mapEntry((k, index) -> k.key, (k, index) -> env.refs[index].finalNode()) //
					.sortByKey(Util::compare) //
					.map((k, v) -> Formatter.display(k) + " = " + Formatter.dump(v));
			StringBuilder sb = new StringBuilder();
			for (String kv : kvs) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(kv);
			}
			return sb.toString();
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
