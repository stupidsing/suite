package suite.immutable;

import static suite.util.Friends.max;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import suite.util.FunUtil.Source;
import suite.util.List_;

public class IRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	private int depth;
	private int weight;
	private IRopeList<T> ts;
	private List<IRope<T>> ropes;

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
		int size = s.length();

		return new IRopeList<>() {
			public int size() {
				return size;
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
		this.weight = ts.size();
		this.ts = ts;
	}

	// minBranchFactor <= ropes.size() && ropes.size() < maxBranchFactor
	public IRope(int depth, List<IRope<T>> ropes) {
		int weight = 0;
		for (IRope<T> rope : ropes)
			weight += rope.weight;
		this.depth = depth;
		this.weight = weight;
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

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return normalize(meld_(rope0, rope1));
	}

	private static <T> List<IRope<T>> meld_(IRope<T> rope0, IRope<T> rope1) {
		int depth = max(rope0.depth, rope1.depth);
		List<IRope<T>> ropes;

		if (rope1.depth < rope0.depth)
			ropes = List_.concat(List_.left(rope0.ropes, -1), meld_(List_.last(rope0.ropes), rope1));
		else if (rope0.depth < rope1.depth)
			ropes = List_.concat(meld_(rope0, List_.first(rope1.ropes)), List_.right(rope1.ropes, 1));
		else if (0 < depth)
			ropes = List_.concat(rope0.ropes, rope1.ropes);
		else {
			IRopeList<T> ts = rope0.ts.concat(rope1.ts);
			int size = ts.size();
			if (maxBranchFactor <= size) {
				IRopeList<T> left = ts.subList(0, minBranchFactor);
				IRopeList<T> right = ts.subList(minBranchFactor, size);
				ropes = List.of(new IRope<>(left), new IRope<>(right));
			} else
				ropes = List.of(new IRope<>(ts));
		}

		List<IRope<T>> list;
		int size1 = ropes.size();

		if (maxBranchFactor <= size1) {
			List<IRope<T>> left = List_.left(ropes, minBranchFactor);
			List<IRope<T>> right = List_.right(ropes, minBranchFactor);
			list = List.of(new IRope<>(depth, left), new IRope<>(depth, right));
		} else
			list = List.of(new IRope<>(depth, ropes));

		return list;
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
				int ix = max(branchFactor, queue1.size());
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
				int ix = max(branchFactor, queue1.size());
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

	private static <T> IRope<T> normalize(List<IRope<T>> ropes) {
		IRope<T> rope = ropes.get(0);
		return ropes.size() != 1 ? new IRope<>(rope.depth + 1, ropes) : rope;
	}

}
