package suite.node.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.IntObjMap;
import suite.adt.IntObjPair;
import suite.adt.Pair;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewriter.NodeHead;
import suite.node.io.Rewriter.NodeRead;
import suite.primitive.PrimitiveSink.IntSink;
import suite.util.FunUtil.Sink;
import suite.util.HashCodeComparable;
import suite.util.Util;

/**
 * The Node.hashCode() method would not permit taking hash code of terms with
 * free references.
 *
 * This method allows such thing by giving aliases, thus "a + .123" and "a +
 * .456" will have same hash keys.
 *
 * @author ywsing
 */
public class TermKey extends HashCodeComparable<TermKey> {

	public class TermVisitor {
		private int nAliases = 0;
		private IntObjMap<Integer> aliases = new IntObjMap<>();
		private IntSink referenceSink;
		private Sink<NodeRead> nrSink;

		public TermVisitor(IntSink referenceSink, Sink<NodeRead> nrSink) {
			this.referenceSink = referenceSink;
			this.nrSink = nrSink;
		}

		public void visit(Node node) {
			if (node instanceof Reference) {
				int id = ((Reference) node).getId();
				referenceSink.sink(aliases.computeIfAbsent(id, any -> nAliases++));
			} else {
				NodeRead nr = NodeRead.of(node);
				for (Pair<Node, Node> p : nr.children) {
					visit(p.t0);
					visit(p.t1);
				}
				nrSink.sink(nr);
			}
		}
	}

	private class TermHasher {
		private int hashCode = 1;

		public TermHasher(Node node) {
			new TermVisitor( //
					i -> hashCode = hashCode * 31 + i //
					, nr -> hashCode = 31 * hashCode + Objects.hash(nr.type, nr.terminal, nr.op) //
			).visit(node);
		}
	}

	private class TermLister {
		private List<IntObjPair<NodeHead>> list = new ArrayList<>();

		public TermLister(Node node) {
			new TermVisitor(i -> list.add(IntObjPair.of(i, null)), nr -> Pair.of(null, nr)).visit(node);
		}

		public boolean equals(Object object) {
			boolean result = Util.clazz(object) == TermLister.class;

			if (result) {
				List<IntObjPair<NodeHead>> list1 = ((TermLister) object).list;
				int size0 = list.size();
				int size1 = list1.size();
				result &= size0 == size1;

				if (result)
					for (int i = 0; result && i < size0; i++) {
						IntObjPair<NodeHead> p0 = list.get(i);
						IntObjPair<NodeHead> p1 = list1.get(i);
						result &= Objects.equals(p0.t0, p1.t0);

						NodeHead nh0 = p0.t1;
						NodeHead nh1 = p1.t1;
						boolean b0 = nh0 != null;
						boolean b1 = nh1 != null;
						result &= b0 == b1;
						if (b0 && b1)
							result &= Objects.equals(nh0.type, nh1.type) //
									&& Objects.equals(nh0.terminal, nh1.terminal) //
									&& Objects.equals(nh0.op, nh1.op);
					}
			}

			return result;
		}

		public int hashCode() {
			int result = 1;
			for (IntObjPair<NodeHead> pair : list) {
				result = 31 * result + Objects.hash(pair.t0);
				if (pair.t1 != null) {
					result = 31 * result + Objects.hash(pair.t1.type);
					result = 31 * result + Objects.hash(pair.t1.terminal);
					result = 31 * result + Objects.hash(pair.t1.op);
				}
			}
			return result;
		}
	}

	public final Node node;

	public TermKey(Node node) {
		this.node = node;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == TermKey.class) {
			Node node1 = ((TermKey) object).node;
			TermLister tl0 = new TermLister(node);
			TermLister tl1 = new TermLister(node1);
			return Objects.equals(tl0, tl1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return new TermHasher(node).hashCode;
	}

}
