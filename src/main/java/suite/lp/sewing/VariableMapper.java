package suite.lp.sewing;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class VariableMapper {

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

	public Generalization g(Fun<Env, Node> fun) {
		Env env = env();
		return new Generalization(fun.apply(env), env);
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
