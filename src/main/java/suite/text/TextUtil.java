package suite.text;

import static suite.util.Friends.min;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.lcs.Lccs;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Read;
import suite.util.List_;
import suite.util.To;

public class TextUtil {

	public class ConflictException extends RuntimeException {
		private static final long serialVersionUID = 1l;
	}

	public Bytes merge(Bytes bytes, Bytes bytesx, Bytes bytesy) {
		var pairsx = diff(bytes, bytesx);
		var pairsy = diff(bytes, bytesy);
		return patch(bytes, merge(pairsx, pairsy));
	}

	public boolean isDiff(List<Pair<Bytes, Bytes>> pairs) {
		return Read.from(pairs).isAny(pair -> pair.t0 != pair.t1);
	}

	public List<Pair<Bytes, Bytes>> diff(Bytes bytesx, Bytes bytesy) {
		var lccs = new Lccs();
		var diff = lccs.lccs(bytesx, bytesy);
		Segment sx = diff.t0, sy = diff.t1;
		int x0 = 0, x1 = sx.start, x2 = sx.end, xx = bytesx.size();
		int y0 = 0, y1 = sy.start, y2 = sy.end, yx = bytesy.size();
		var common = bytesx.range(x1, x2);

		if (!sx.isEmpty() && !sy.isEmpty()) {
			var patch = new ArrayList<Pair<Bytes, Bytes>>();
			patch.addAll(diff(bytesx.range(x0, x1), bytesy.range(y0, y1)));
			patch.add(Pair.of(common, common));
			patch.addAll(diff(bytesx.range(x2, xx), bytesy.range(y2, yx)));
			return patch;
		} else if (!bytesx.isEmpty() || !bytesy.isEmpty())
			return List.of(Pair.of(bytesx, bytesy));
		else
			return List.of();
	}

	public Bytes patch(Bytes bytes, List<Pair<Bytes, Bytes>> pairs) {
		var bb = new BytesBuilder();
		var p = 0;
		for (var pair : pairs) {
			var p1 = p + pair.t0.size();
			if (Objects.equals(bytes.range(p, p1), pair.t0))
				bb.append(pair.t1);
			else
				throw new ConflictException();
			p = p1;
		}
		return bb.toBytes();
	}

	public List<Pair<Bytes, Bytes>> merge(List<Pair<Bytes, Bytes>> pairsx, List<Pair<Bytes, Bytes>> pairsy) {
		return merge(pairsx, pairsy, false);
	}

	public List<Pair<Bytes, Bytes>> merge( //
			List<Pair<Bytes, Bytes>> pairsx, //
			List<Pair<Bytes, Bytes>> pairsy, //
			boolean isDetectSameChanges) {
		var isEmptyx = pairsx.isEmpty();
		var isEmptyy = pairsy.isEmpty();

		if (!isEmptyx || !isEmptyy) {
			var phx = !isEmptyx ? pairsx.get(0) : Pair.of(Bytes.empty, Bytes.empty);
			var phy = !isEmptyy ? pairsy.get(0) : Pair.of(Bytes.empty, Bytes.empty);
			var ptx = !isEmptyx ? List_.right(pairsx, 1) : pairsx;
			var pty = !isEmptyy ? List_.right(pairsy, 1) : pairsy;

			var c = min(phx.t0.size(), phy.t0.size());
			var commonx = phx.t0.range(0, c);
			var commony = phy.t0.range(0, c);

			if (Objects.equals(commonx, commony)) {
				int s0, s1;
				Pair<Bytes, Bytes> pair;
				List<Pair<Bytes, Bytes>> pairs;

				if (isDetectSameChanges //
						&& phx.t0 != phx.t1 //
						&& phy.t0 != phy.t1 //
						&& 0 < (s0 = detectSame(phx.t0, phy.t0)) //
						&& 0 < (s1 = detectSame(phx.t1, phy.t1))) {
					pair = Pair.of(phx.t0.range(0, s0), phx.t1.range(0, s1));
					pairs = merge( //
							cons(Pair.of(phx.t0.range(s0), phx.t1.range(s1)), ptx), //
							cons(Pair.of(phy.t0.range(s0), phy.t1.range(s1)), pty), //
							isDetectSameChanges);
				} else if (phx.t0 != phx.t1) {
					pair = phx;
					pairs = merge(ptx, cons(skip(phy, c), pty), isDetectSameChanges);
				} else if (phy.t0 != phy.t1) {
					pair = phy;
					pairs = merge(cons(skip(phx, c), ptx), pty, isDetectSameChanges);
				} else {
					pair = Pair.of(commonx, commonx);
					pairs = merge(cons(skip(phx, c), ptx), cons(skip(phy, c), pty), isDetectSameChanges);
				}

				return cons(pair, pairs);
			} else
				throw new ConflictException();
		} else
			return List.of();
	}

	private int detectSame(Bytes x, Bytes y) {
		var s = min(x.size(), y.size());
		var i = 0;
		while (i < s && x.get(i) == y.get(i))
			i++;
		return i;
	}

	public Bytes fromTo(List<Pair<Bytes, Bytes>> pairs, boolean isFrom) {
		var bb = new BytesBuilder();
		for (var pair : pairs)
			if (pair != null)
				bb.append(isFrom ? pair.t0 : pair.t1);
			else
				return null;
		return bb.toBytes();
	}

	public String toString(List<Pair<Bytes, Bytes>> pairs) {
		var sb = new StringBuilder();
		for (var pair : pairs)
			if (pair.t0 == pair.t1)
				sb.append(To.string(pair.t0));
			else
				sb.append("[" + To.string(pair.t0) + "|" + To.string(pair.t1) + "]");
		return sb.toString();
	}

	private List<Pair<Bytes, Bytes>> cons(Pair<Bytes, Bytes> ph, List<Pair<Bytes, Bytes>> pt) {
		if (!ph.t0.isEmpty() || !ph.t1.isEmpty())
			return List_.concat(List.of(ph), pt);
		else
			return pt;
	}

	private Pair<Bytes, Bytes> skip(Pair<Bytes, Bytes> pair, int c) {
		if (pair.t0 == pair.t1 && c <= pair.t0.size()) {
			var bytes = pair.t0.range(c);
			return Pair.of(bytes, bytes);
		} else
			throw new ConflictException();
	}

}
