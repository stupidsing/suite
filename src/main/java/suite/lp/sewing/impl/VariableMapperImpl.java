package suite.lp.sewing.impl;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Object_;

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
			return Read //
					.from2(variableIndices) //
					.map2((k, index) -> k.key, (k, index) -> env.refs[index].finalNode()) //
					.sortByKey(Object_::compare) //
					.map((k, v) -> Formatter.display(k) + " = " + Formatter.dump(v)) //
					.collect(As.joinedBy(", "));
		}

		public Node getVariable(Node variable) {
			return env.refs[getVariableIndex(variable)];
		}
	}

	public Env env() {
		return Env.empty(nVariables);
	}

	public int findVariableIndex(Node variable) {
		return variableIndices.computeIfAbsent(IdentityKey.of(variable), any -> nVariables++);
	}

	public Integer getVariableIndex(Node variable) {
		return variableIndices.get(IdentityKey.of(variable));
	}

}
