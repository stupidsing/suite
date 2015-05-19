package suite.node.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import suite.adt.Pair;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewriter.NodeRead;
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

		public int hash(Node node) {
			node = node.finalNode();

			if (node instanceof Reference) {
				int id = ((Reference) node).getId();
				return aliases.computeIfAbsent(id, any -> nAliases++);
			} else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				int result = 1;
				result = 31 * result + hash(tree.getLeft());
				result = 31 * result + Objects.hashCode(tree.getOperator());
				result = 31 * result + hash(tree.getRight());
				return result;
			} else {
				NodeRead nr = NodeRead.of(node);
				int result = Objects.hash(nr.type, nr.terminal, nr.op);
				for (Pair<Node, Node> p : nr.children)
					result = 31 * result + hash(p.t0) ^ hash(p.t1);
				return result;
			}
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
