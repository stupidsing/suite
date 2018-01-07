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
import suite.util.String_;

public class VariableMapper<K> {

	private Map<K, Integer> indices = new IdentityHashMap<>();
	private int nVariables;

	public static class NodeEnv<K> {
		private Map<K, Integer> indices;
		public final Node node;
		public final Env env;

		private NodeEnv(Map<K, Integer> indices, Node node, Env env) {
			this.indices = indices;
			this.node = node;
			this.env = env;
		}

		public String dumpVariables() {
			Reference[] refs = env.refs;
			return Read //
					.from2(indices) //
					.map2((key, index) -> display(key), (key, index) -> refs[index].finalNode()) //
					.sortByKey(String_::compare) //
					.map((k, v) -> k + " = " + Formatter.dump(v)) //
					.collect(As.joinedBy(", "));
		}

		public Node getVariable(K variable) {
			return env.refs[indices.get(variable)];
		}

		private String display(K key) {
			return key instanceof Node ? Formatter.display((Node) key) : key.toString();
		}
	}

	public Source<NodeEnv<K>> g(Fun<Env, Node> fun) {
		return () -> {
			Env env = env();
			return new NodeEnv<>(indices, fun.apply(env), env);
		};
	}

	public Env env() {
		return Env.empty(nVariables);
	}

	public int computeIndex(K variable) {
		return indices.computeIfAbsent(variable, any -> nVariables++);
	}

	public Integer getIndex(K variable) {
		return indices.get(variable);
	}

}
