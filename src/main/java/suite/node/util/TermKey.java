package suite.node.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.util.HashCodeComparable;

/**
 * The Node.hashCode() method would not permit taking hash code of terms with
 * free references.
 *
 * This method allows such thing by giving aliases, thus "a + .123" and
 * "a + .456" will have same hashes.
 *
 * @author ywsing
 */
public class TermKey extends HashCodeComparable<TermKey> {

	public static class TermHasher {
		private int nAliases = 0;
		private Map<Integer, Integer> aliases = new HashMap<>();

		public Integer hash(Node node) {
			node = node.finalNode();

			if (node instanceof Dict) {
				Map<Node, Reference> map = ((Dict) node).map;
				int result = 0;
				for (Entry<Node, Reference> e : map.entrySet())
					result = 31 * hash(e.getKey()) + hash(e.getValue());
				return result;
			} else if (node instanceof Reference) {
				int id = ((Reference) node).getId();
				return aliases.computeIfAbsent(id, any -> nAliases++);
			} else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				int result = 1;
				result = 31 * result + hash(tree.getLeft());
				result = 31 * result + Objects.hashCode(tree.getOperator());
				result = 31 * result + hash(tree.getRight());
				return result;
			} else if (node instanceof Tuple) {
				Tuple tuple = (Tuple) node;
				int result = 1;
				for (Node n : tuple.nodes)
					result = 31 * result + hash(n);
				return result;
			} else
				return node.hashCode();
		}
	}

	public final Node node;

	public TermKey(Node node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		return new TermHasher().hash(node);
	}

	@Override
	public boolean equals(Object object) {
		if (object.getClass() == TermKey.class) {
			Node node1 = ((TermKey) object).node;
			return Objects.equals(new TermHasher().hash(node), new TermHasher().hash(node1));
		} else
			return false;
	}

}
