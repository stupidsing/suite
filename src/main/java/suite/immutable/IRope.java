package suite.immutable;

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
	private List<T> ts;
	private List<IRope<T>> ropes;

	// minBranchFactor <= ts.size() && ts.size() < maxBranchFactor
	public IRope(List<T> ts) {
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
		if (0 < depth) {
			int index = 0, w;
			IRope<T> rope;
			while (!(p <= (w = (rope = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			IRope<T> rope_ = rope.left(p);
			if (Boolean.TRUE) {
				Deque<IRope<T>> deque = new ArrayDeque<>();
				for (int i = 0; i < index; i++)
					deque.push(ropes.get(i));
				return meldLeft(deque, rope_);
			} else {
				for (int i = index - 1; 0 <= index; i--)
					rope_ = meld(ropes.get(i), rope_);
				return rope_;
			}
		} else
			return new IRope<>(List_.left(ts, p));
	}

	// 0 <= p && p < weight
	public IRope<T> right(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> rope;
			while (!(p < (w = (rope = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			IRope<T> rope_ = rope.right(p);
			if (Boolean.TRUE) {
				Deque<IRope<T>> deque = new ArrayDeque<>();
				for (int i = ropes.size() - 1; index < i; i--)
					deque.push(ropes.get(i));
				return meldRight(rope_, deque);
			} else {
				for (int i = index + 1; index < ropes.size(); i++)
					rope_ = meld(rope_, ropes.get(i));
				return rope_;
			}
		} else
			return new IRope<>(List_.right(ts, p));
	}

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return normalize(meld_(rope0, rope1));
	}

	private static <T> List<IRope<T>> meld_(IRope<T> rope0, IRope<T> rope1) {
		int depth = Math.max(rope0.depth, rope1.depth);
		List<IRope<T>> ropes;

		if (rope1.depth < rope0.depth)
			ropes = List_.concat(List_.left(rope0.ropes, -1), meld_(List_.last(rope0.ropes), rope1));
		else if (rope0.depth < rope1.depth)
			ropes = List_.concat(meld_(rope0, List_.first(rope1.ropes)), List_.right(rope1.ropes, 1));
		else if (0 < depth)
			ropes = List_.concat(rope0.ropes, rope1.ropes);
		else {
			List<T> ts = List_.concat(rope0.ts, rope1.ts);
			if (maxBranchFactor <= ts.size()) {
				List<T> left = List_.left(ts, minBranchFactor);
				List<T> right = List_.right(ts, minBranchFactor);
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

	private IRope<T> meldLeft(Deque<IRope<T>> queue, IRope<T> rope) {
		int branchFactor = minBranchFactor;

		while (true) {
			int depth = rope.depth;

			Deque<IRope<T>> queue1 = new ArrayDeque<>();
			queue1.push(rope);

			Source<IRope<T>> pack = () -> {
				int ix = Math.max(branchFactor, queue1.size());
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

	private IRope<T> meldRight(IRope<T> rope, Deque<IRope<T>> queue) {
		int branchFactor = minBranchFactor;

		while (true) {
			int depth = rope.depth;

			Deque<IRope<T>> queue1 = new ArrayDeque<>();
			queue1.push(rope);

			Source<IRope<T>> pack = () -> {
				int ix = Math.max(branchFactor, queue1.size());
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
