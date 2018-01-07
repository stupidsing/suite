package suite.lp.sewing;

import java.util.IdentityHashMap;
import java.util.Map;

import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;

public class VariableMapper {

	private Map<Node, Integer> indices = new IdentityHashMap<>();
	private int nVariables;

	public class NodeEnv {
		public final Node node;
		public final Env env;

		private NodeEnv(Node node, Env env) {
			this.node = node;
			this.env = env;
		}

		public String dumpVariables() {
			Reference[] refs = env.refs;
			return Read //
					.from2(indices) //
					.mapValue(index -> refs[index].finalNode()) //
					.sortByKey(Object_::compare) //
					.map((k, v) -> Formatter.display(k) + " = " + Formatter.dump(v)) //
					.collect(As.joinedBy(", "));
		}

		public Node getVariable(Node variable) {
			return env.refs[getIndex(variable)];
		}
	}

	public Source<NodeEnv> g(Fun<Env, Node> fun) {
		return () -> {
			Env env = env();
			return new NodeEnv(fun.apply(env), env);
		};
	}

	public Env env() {
		return Env.empty(nVariables);
	}

	public int computeIndex(Node variable) {
		return indices.computeIfAbsent(variable, any -> nVariables++);
	}

	public Integer getIndex(Node variable) {
		return indices.get(variable);
	}

}
