package suite.immutable;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import suite.inspect.Dump;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Source;
import suite.util.List_;

public class IRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	public final int depth;
	public final int weight;
	public final IRopeList<T> ts;
	public final List<IRope<T>> ropes;

	public interface IRopeList<T> {
		public int size();

		public T get(int index);

		public IRopeList<T> subList(int start, int end);

		public IRopeList<T> concat(IRopeList<T> list);

		public default IRopeList<T> left(int p) {
			return subList(0, p);
		}

		public default IRopeList<T> right(int p) {
			return subList(p, size());
		}
	}

	public static IRopeList<Character> ropeList(String s) {
		return new IRopeList<>() {
			public int size() {
				return s.length();
			}

			public Character get(int index) {
				return s.charAt(index);
			}

			public IRopeList<Character> subList(int i0, int ix) {
				return ropeList(s.substring(i0, ix));
			}

			public IRopeList<Character> concat(IRopeList<Character> list) {
				return ropeList(s + list.toString());
			}

			public String toString() {
				return s;
			}
		};
	}

	// minBranchFactor <= ts.size() && ts.size() < maxBranchFactor
	public IRope(IRopeList<T> ts) {
		this.depth = 0;
		this.weight = ts.size();
		this.ts = ts;
		this.ropes = null;
	}

	// minBranchFactor <= ropes.size() && ropes.size() < maxBranchFactor
	public IRope(int depth, List<IRope<T>> ropes) {
		int weight = 0;
		for (IRope<T> rope : ropes)
			weight += rope.weight;
		this.depth = depth;
		this.weight = weight;
		this.ts = null;
		this.ropes = ropes;
	}

	// 0 <= p && p < weight
	public T at(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> rope;
			while (!(p < (w = (rope = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			return rope.at(p);
		} else
			return ts.get(p);
	}

	// 0 < p && p <= weight
	public IRope<T> left(int p) {
		return left(this, p);
	}

	// 0 <= p && p < weight
	public IRope<T> right(int p) {
		return right(this, p);
	}

	public IRope<T> validateRoot() {
		return validate(true) ? this : null;
	}

	public boolean validate(boolean isRoot) {
		Streamlet<IRope<T>> rs;
		int s;
		return (false //
				|| depth == 0 //
						&& weight == (s = ts.size()) //
						&& s < maxBranchFactor //
						&& ropes == null //
				|| (rs = Read.from(ropes)) != null //
						&& rs.isAll(rope -> rope.depth + 1 == depth) // //
						&& rs.toInt(Obj_Int.sum(rope -> rope.weight)) == weight //
						&& ts == null //
						&& (s = rs.size()) < maxBranchFactor //
						&& rs.isAll(rope -> rope.validate(false))) //
				&& (isRoot || minBranchFactor <= s) ? true : Fail.t(Dump.object(this));
	}

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return newRoot(meld_(rope0, rope1)).validateRoot();
	}

	private static <T> List<IRope<T>> meld_(IRope<T> rope0, IRope<T> rope1) {
		int depth0 = rope0.depth;
		int depth1 = rope1.depth;
		int depth = max(depth0, depth1);

		if (depth0 != depth1) {
			List<IRope<T>> ropes;

			if (depth1 < depth0)
				ropes = List_.concat(List_.left(rope0.ropes, -1), meld_(List_.last(rope0.ropes), rope1));
			else if (depth0 < depth1)
				ropes = List_.concat(meld_(rope0, List_.first(rope1.ropes)), List_.right(rope1.ropes, 1));
			else
				ropes = List_.concat(rope0.ropes, rope1.ropes);

			List<IRope<T>> list;
			int size = ropes.size();

			if (maxBranchFactor <= size) {
				int p = size / 2;
				List<IRope<T>> left = List_.left(ropes, p);
				List<IRope<T>> right = List_.right(ropes, p);
				list = List.of(new IRope<>(depth, left), new IRope<>(depth, right));
			} else
				list = List.of(new IRope<>(depth, ropes));

			return list;
		} else {
			IRopeList<T> ts = rope0.ts.concat(rope1.ts);
			int size = ts.size();

			if (maxBranchFactor <= size) {
				int p = size / 2;
				IRopeList<T> left = ts.subList(0, p);
				IRopeList<T> right = ts.subList(p, size);
				return List.of(new IRope<>(left), new IRope<>(right));
			} else
				return List.of(new IRope<>(ts));
		}
	}

	private static <T> IRope<T> left(IRope<T> rope, int p) {
		Deque<IRope<T>> deque = new ArrayDeque<>();
		List<IRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			IRope<T> rope_;
			while (!(p <= (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (int i = 0; i < index; i++)
				deque.push(ropes.get(i));
			rope = rope_;
		}

		return meldLeft(deque, new IRope<>(rope.ts.subList(0, p)));
	}

	private static <T> IRope<T> right(IRope<T> rope, int p) {
		Deque<IRope<T>> deque = new ArrayDeque<>();
		List<IRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			IRope<T> rope_;
			while (!(p < (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (int i = ropes.size() - 1; index < i; i--)
				deque.push(ropes.get(i));
			rope = rope_;
		}

		return meldRight(new IRope<>(rope.ts.subList(p, rope.weight)), deque);
	}

	private static <T> IRope<T> meldLeft(Deque<IRope<T>> queue, IRope<T> rope) {
		int branchFactor = minBranchFactor;

		while (true) {
			Deque<IRope<T>> queue1 = new ArrayDeque<>();
			int depth = rope.depth;

			queue1.push(rope);

			Source<IRope<T>> pack = () -> {
				int ix = min(branchFactor, queue1.size());
				List<IRope<T>> ropes = new ArrayList<>(Collections.nCopies(ix, null));
				for (int i = 0; i < ix; i++)
					ropes.set(i, queue1.pop());
				return new IRope<>(depth + 1, ropes);
			};

			while (queue1.size() < branchFactor) {
				IRope<T> rope1 = queue.pollFirst();

				if (rope1 != null)
					new Object() {
						public void add(IRope<T> rope_) {
							if (depth < rope_.depth) {
								List<IRope<T>> ropes = rope_.ropes;
								for (int i = ropes.size() - 1; 0 <= i; i--)
									add(ropes.get(i));
							} else
								queue1.push(rope_);
						}
					}.add(rope1);
				else
					return pack.source();
			}

			rope = pack.source();
		}
	}

	private static <T> IRope<T> meldRight(IRope<T> rope, Deque<IRope<T>> queue) {
		int branchFactor = minBranchFactor;

		while (true) {
			Deque<IRope<T>> queue1 = new ArrayDeque<>();
			int depth = rope.depth;

			queue1.push(rope);

			Source<IRope<T>> pack = () -> {
				int ix = min(branchFactor, queue1.size());
				List<IRope<T>> ropes = new ArrayList<>(Collections.nCopies(ix, null));
				for (int i = 0; i < ix; i++)
					ropes.set(ix - i - 1, queue1.pop());
				return new IRope<>(depth + 1, ropes);
			};

			while (queue1.size() < branchFactor) {
				IRope<T> rope1 = queue.pollFirst();

				if (rope1 != null)
					new Object() {
						public void add(IRope<T> rope_) {
							if (depth < rope_.depth) {
								List<IRope<T>> ropes = rope_.ropes;
								for (int i = 0; i < ropes.size(); i++)
									add(ropes.get(i));
							} else
								queue1.push(rope_);
						}
					}.add(rope1);
				else
					return pack.source();
			}

			rope = pack.source();
		}
	}

	private static <T> IRope<T> newRoot(List<IRope<T>> ropes) {
		IRope<T> rope = ropes.get(0);
		return ropes.size() != 1 ? new IRope<>(rope.depth + 1, ropes) : rope;
	}

}
