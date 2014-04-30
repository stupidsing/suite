package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;

/**
 * The Node.hashCode() method would not permit taking hash code of terms with
 * free references.
 * 
 * This method allows such thing by giving aliases, thus "a + .123" and
 * "a + .456" will have same hashes.
 *
 * @author ywsing
 */
public class Hasher {

	int nAliases = 0;
	private Map<Integer, Integer> aliases = new HashMap<>();

	public static class HashedTerm {
		private Node node;

		public HashedTerm(Node node) {
			this.node = node;
		}

		@Override
		public int hashCode() {
			return new Hasher().hash(node);
		}

		@Override
		public boolean equals(Object object) {
			return object.getClass() == HashedTerm.class ? Objects.equals(node, ((HashedTerm) object).node) : false;
		}

		public Node getNode() {
			return node;
		}
	}

	public Integer hash(Node node) {
		node = node.finalNode();

		if (node instanceof Reference) {
			int id = ((Reference) node).getId();
			Integer alias = aliases.get(id);
			if (alias == null)
				aliases.put(id, alias = nAliases++);
			return alias;
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			int result = 1;
			result = 31 * result + hash(tree.getLeft());
			result = 31 * result + Objects.hashCode(tree.getOperator());
			result = 31 * result + hash(tree.getRight());
			return result;
		} else
			return node.hashCode();
	}

}
