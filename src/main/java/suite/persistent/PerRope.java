package suite.persistent;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.List;

import primal.fp.Funs.Iterate;
import primal.primitive.IntInt_Obj;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.List_;

public class PerRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	public final int depth;
	public final int weight;
	public final IRopeList<T> ts;
	public final List<PerRope<T>> ropes;

	public static class IRopeList<T> {
		public int size;
		public Int_Obj<T> get;
		public IntInt_Obj<IRopeList<T>> subList;
		public Iterate<IRopeList<T>> concat;

		public static IRopeList<Character> of(String s) {
			return ropeList(PerRope.of(ropeList(s)));
		}

		public IRopeList(int size, Int_Obj<T> get, IntInt_Obj<IRopeList<T>> subList, Iterate<IRopeList<T>> concat) {
			this.size = size;
			this.get = get;
			this.subList = subList;
			this.concat = concat;
		}

		public IRopeList<T> left(int p) {
			return subList.apply(0, p);
		}

		public IRopeList<T> right(int p) {
			return subList.apply(p, size);
		}
	}

	public static <T> PerRope<T> of(IRopeList<T> ts) {
		var rope = new PerRope<T>(ts.left(0));
		var size = ts.size;
		var p = 0;
		while (p < size) {
			var p1 = min(p + minBranchFactor, size);
			rope = meld(rope, new PerRope<T>(ts.subList.apply(p, p1)));
			p = p1;
		}
		return rope;
	}

	private static IRopeList<Character> ropeList(String s) {
		return new IRopeList<>( //
				s.length(), //
				s::charAt, //
				(i0, ix) -> ropeList(s.substring(i0, ix)), //
				list -> ropeList(s + list.toString())) {
			public String toString() {
				return s;
			}
		};
	}

	private static <T> IRopeList<T> ropeList(PerRope<T> rope) {
		class W extends IRopeList<T> {
			private PerRope<T> rope_ = rope;

			private W() {
				super(rope.weight, rope::at, (i0, ix) -> ropeList(rope.left(ix).right(i0)), null);
			}
		}

		var ropeList = new W();
		ropeList.rope_ = rope;
		ropeList.concat = list -> ropeList(meld(rope, ((W) list).rope_));
		return ropeList;
	}

	// minBranchFactor <= ts.size() && ts.size() < maxBranchFactor
	public PerRope(IRopeList<T> ts) {
		this.depth = 0;
		this.weight = ts.size;
		this.ts = ts;
		this.ropes = null;
		validateRoot();
	}

	// minBranchFactor <= ropes.size() && ropes.size() < maxBranchFactor
	public PerRope(int depth, List<PerRope<T>> ropes) {
		var weight = 0;
		for (var rope : ropes)
			weight += rope.weight;
		this.depth = depth;
		this.weight = weight;
		this.ts = null;
		this.ropes = ropes;
		validateRoot();
	}

	// 0 <= p && p < weight
	public T at(int p) {
		if (0 < depth) {
			int index = 0, w;
			PerRope<T> rope;
			while (!(p < (w = (rope = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			return rope.at(p);
		} else
			return ts.get.apply(p);
	}

	public PerRope<T> left(int p) {
		return 0 < p ? left_(p) : empty();
	}

	public PerRope<T> right(int p) {
		return p < weight ? right_(p) : empty();
	}

	private PerRope<T> empty() {
		var rope = this;
		List<PerRope<T>> ropes;
		while ((ropes = rope.ropes) != null)
			rope = ropes.get(0);
		return new PerRope<T>(rope.ts.left(0));
	}

	// 0 < p && p <= weight
	private PerRope<T> left_(int p) {
		var deque = new ArrayDeque<PerRope<T>>();
		var rope = this;
		List<PerRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			PerRope<T> rope_;
			while (!(p <= (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (var i = 0; i < index; i++)
				deque.push(ropes.get(i));
			rope = rope_;
		}

		var rope1 = new PerRope<>(rope.ts.subList.apply(0, p));
		PerRope<T> rope_;

		while ((rope_ = deque.pollFirst()) != null)
			rope1 = meld(rope_, rope1);

		return rope1;
	}

	// 0 <= p && p < weight
	private PerRope<T> right_(int p) {
		var deque = new ArrayDeque<PerRope<T>>();
		var rope = this;
		List<PerRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			PerRope<T> rope_;
			while (!(p < (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (var i = ropes.size() - 1; index < i; i--)
				deque.push(ropes.get(i));
			rope = rope_;
		}
		var rope1 = new PerRope<>(rope.ts.subList.apply(p, rope.weight));
		PerRope<T> rope_;

		while ((rope_ = deque.pollFirst()) != null)
			rope1 = meld(rope1, rope_);

		return rope1;
	}

	public PerRope<T> validateRoot() {
		return validate(true) ? this : null;
	}

	public boolean validate(boolean isRoot) {
		Streamlet<PerRope<T>> rs;
		int s;
		return (false //
				|| depth == 0 //
						&& weight == (s = ts.size) //
						&& s < maxBranchFactor //
						&& ropes == null //
				|| (rs = Read.from(ropes)) != null //
						&& rs.isAll(rope -> rope.depth + 1 == depth) //
						&& rs.toInt(Obj_Int.sum(rope -> rope.weight)) == weight //
						&& ts == null //
						&& (s = rs.size()) < maxBranchFactor //
						&& rs.isAll(rope -> rope.validate(false))) //
				&& (isRoot || minBranchFactor <= s) ? true : fail();
	}

	public static <T> PerRope<T> meld(PerRope<T> rope0, PerRope<T> rope1) {
		return newRoot(meld_(rope0, rope1));
	}

	private static <T> List<PerRope<T>> meld_(PerRope<T> rope0, PerRope<T> rope1) {
		var depth0 = rope0.depth;
		var depth1 = rope1.depth;
		var depth = max(depth0, depth1);

		if (0 < depth) {
			List<PerRope<T>> ropes;

			if (depth1 < depth0)
				ropes = List_.concat(List_.left(rope0.ropes, -1), meld_(List_.last(rope0.ropes), rope1));
			else if (depth0 < depth1)
				ropes = List_.concat(meld_(rope0, List_.first(rope1.ropes)), List_.right(rope1.ropes, 1));
			else
				ropes = List_.concat(rope0.ropes, rope1.ropes);

			List<PerRope<T>> list;
			var size = ropes.size();

			if (maxBranchFactor <= size) {
				var p = size / 2;
				var left = List_.left(ropes, p);
				var right = List_.right(ropes, p);
				list = List.of(new PerRope<>(depth, left), new PerRope<>(depth, right));
			} else
				list = List.of(new PerRope<>(depth, ropes));

			return list;
		} else {
			var ts = rope0.ts.concat.apply(rope1.ts);
			var size = ts.size;

			if (maxBranchFactor <= size) {
				var p = size / 2;
				var left = ts.left(p);
				var right = ts.right(p);
				return List.of(new PerRope<>(left), new PerRope<>(right));
			} else
				return List.of(new PerRope<>(ts));
		}
	}

	private static <T> PerRope<T> newRoot(List<PerRope<T>> ropes) {
		var rope = ropes.get(0);
		return ropes.size() != 1 ? new PerRope<>(rope.depth + 1, ropes) : rope;
	}

}
