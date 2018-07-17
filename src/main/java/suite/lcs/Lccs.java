package suite.lcs;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.primitive.Bytes;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.adt.set.IntSet;
import suite.text.RollingHash;
import suite.text.Segment;

/**
 * Longest common continuous subsequence, using a simple rolling hash method.
 *
 * @author ywsing
 */
public class Lccs {

	private RollingHash rh = new RollingHash();

	public Pair<Segment, Segment> lccs(Bytes bytes0, Bytes bytes1) {
		var size0 = bytes0.size();
		var size1 = bytes1.size();
		var rollingSize = max(1, min(size0, size1));
		var longest = IntObjPair.<Pair<Segment, Segment>> of(Integer.MIN_VALUE, null);

		while (longest.t1 == null) {
			var segmentLists0 = hashSegments(bytes0, rollingSize);
			var segmentLists1 = hashSegments(bytes1, rollingSize);
			var keys0 = segmentLists0.streamlet().keys().toSet();
			var keys1 = segmentLists1.streamlet().keys().toSet();
			var keys = IntSet.intersect(keys0, keys1).streamlet().toArray();

			for (var key : keys)
				for (var segment0 : segmentLists0.get(key))
					for (var segment1 : segmentLists1.get(key)) {
						int start0 = segment0.start, end0 = segment0.end;
						int start1 = segment1.start, end1 = segment1.end;
						var b0 = bytes0.range(start0, end0);
						var b1 = bytes1.range(start1, end1);

						if (Objects.equals(b0, b1)) {
							var ix = min(size0 - start0, size1 - start1);
							var i = rollingSize;
							while (i < ix && bytes0.get(start0 + i) == bytes1.get(start1 + i))
								i++;
							if (longest.t0 < i)
								longest.update(i, Pair.of(Segment.of(start0, start0 + i), Segment.of(start1, start1 + i)));
						}
					}

			if ((rollingSize /= 2) == 0)
				return Pair.of(Segment.of(0, 0), Segment.of(0, 0));
		}

		return longest.t1;
	}

	private IntObjMap<List<Segment>> hashSegments(Bytes bytes, int rollingSize) {
		IntObjMap<List<Segment>> segments = new IntObjMap<>();
		int hash = rh.hash(bytes.range(0, rollingSize - 1));
		var size = bytes.size();

		for (var pos = 0; pos <= size - rollingSize; pos++) {
			var pos_ = pos;
			hash = rh.roll(hash, bytes.get(pos_ + rollingSize - 1));
			segments.computeIfAbsent(hash, segment0 -> new ArrayList<>()).add(Segment.of(pos_, pos_ + rollingSize));
			hash = rh.unroll(hash, bytes.get(pos_), rollingSize);
		}

		return segments;
	}

}
