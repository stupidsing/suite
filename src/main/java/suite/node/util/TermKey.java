package suite.node.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewrite_.NodeHead;
import suite.node.io.Rewrite_.NodeRead;
import suite.object.Object_;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.FunUtil.Sink;

/**
 * The Node.hashCode() method would not permit taking hash code of terms with
 * free references.
 *
 * This method allows such thing by giving aliases, thus "a + .123" and "a +
 * .456" will have same hash keys.
 *
 * @author ywsing
 */
public class TermKey implements Comparable<TermKey> {

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
				var id = ((Reference) node).getId();
				referenceSink.f(aliases.computeIfAbsent(id, any -> nAliases++));
			} else {
				var nr = NodeRead.of(node);
				for (var p : nr.children) {
					visit(p.t0);
					visit(p.t1);
				}
				nrSink.f(nr);
			}
		}
	}

	private class TermHasher {
		private int h = 7;

		public TermHasher(Node node) {
			new TermVisitor( //
					i -> h = h * 31 + i //
					, nr -> h = h * 31 + Objects.hash(nr.type, nr.terminal, nr.op) //
			).visit(node);
		}
	}

	private class TermLister {
		private List<IntObjPair<NodeHead>> list = new ArrayList<>();

		public TermLister(Node node) {
			new TermVisitor(i -> list.add(IntObjPair.of(i, null)), nr -> Pair.of(null, nr)).visit(node);
		}

		public boolean equals(Object object) {
			var b = Object_.clazz(object) == TermLister.class;

			if (b) {
				var list1 = ((TermLister) object).list;
				var size0 = list.size();
				var size1 = list1.size();
				b &= size0 == size1;

				if (b)
					for (var i = 0; b && i < size0; i++) {
						var p0 = list.get(i);
						var p1 = list1.get(i);
						b &= Objects.equals(p0.t0, p1.t0);

						var nh0 = p0.t1;
						var nh1 = p1.t1;
						var b0 = nh0 != null;
						var b1 = nh1 != null;
						b &= b0 == b1;
						if (b0 && b1)
							b &= Objects.equals(nh0.type, nh1.type) //
									&& Objects.equals(nh0.terminal, nh1.terminal) //
									&& Objects.equals(nh0.op, nh1.op);
					}
			}

			return b;
		}

		public int hashCode() {
			var h = 7;
			for (var pair : list) {
				h = h * 31 + Objects.hash(pair.t0);
				if (pair.t1 != null) {
					h = h * 31 + Objects.hash(pair.t1.type);
					h = h * 31 + Objects.hash(pair.t1.terminal);
					h = h * 31 + Objects.hash(pair.t1.op);
				}
			}
			return h;
		}
	}

	public final Node node;

	public TermKey(Node node) {
		this.node = node;
	}

	@Override
	public int compareTo(TermKey other) {
		return Integer.compare(hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == TermKey.class) {
			var node1 = ((TermKey) object).node;
			var tl0 = new TermLister(node);
			var tl1 = new TermLister(node1);
			return Objects.equals(tl0, tl1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return new TermHasher(node).h;
	}

}
