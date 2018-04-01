package suite.lcs;

import static suite.util.Friends.min;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.primitive.Bytes;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.adt.set.IntSet;
import suite.text.RollingHashUtil;
import suite.text.Segment;

/**
 * Longest common continuous subsequence, using a simple rolling hash method.
 *
 * @author ywsing
 */
public class Lccs {

	private RollingHashUtil rh = new RollingHashUtil();

	public Pair<Segment, Segment> lccs(Bytes bytes0, Bytes bytes1) {
		int size0 = bytes0.size();
		int size1 = bytes1.size();
		int rollingSize = min(size0, size1);

		if (0 < rollingSize) {
			IntObjPair<Pair<Segment, Segment>> longest = IntObjPair.of(Integer.MIN_VALUE, null);

			while (longest.t1 == null) {
				IntObjMap<List<Segment>> segmentLists0 = hashSegments(bytes0, rollingSize);
				IntObjMap<List<Segment>> segmentLists1 = hashSegments(bytes1, rollingSize);
				IntSet keys0 = segmentLists0.streamlet().keys().toSet();
				IntSet keys1 = segmentLists1.streamlet().keys().toSet();
				int[] keys = IntSet.intersect(keys0, keys1).streamlet().toArray();

				for (int key : keys)
					for (Segment segment0 : segmentLists0.get(key))
						for (Segment segment1 : segmentLists1.get(key)) {
							int start0 = segment0.start, end0 = segment0.end;
							int start1 = segment1.start, end1 = segment1.end;
							Bytes b0 = bytes0.range(start0, end0);
							Bytes b1 = bytes1.range(start1, end1);

							if (Objects.equals(b0, b1)) {
								int ix = Math.min(size0 - start0, size1 - start1);
								int i = rollingSize;
								while (i < ix && bytes0.get(start0 + i) == bytes1.get(start1 + i))
									i++;
								if (longest.t0 < i)
									longest.update(i, Pair.of(Segment.of(start0, start0 + i), Segment.of(start1, start1 + i)));
							}
						}

				rollingSize /= 2;
			}

			return longest.t1;
		} else
			return Pair.of(Segment.of(0, 0), Segment.of(0, 0));
	}

	private IntObjMap<List<Segment>> hashSegments(Bytes bytes, int rollingSize) {
		IntObjMap<List<Segment>> segments = new IntObjMap<>();
		int hash = rh.hash(bytes.range(0, rollingSize - 1));
		int size = bytes.size();

		for (int pos = 0; pos <= size - rollingSize; pos++) {
			int pos_ = pos;
			hash = rh.roll(hash, bytes.get(pos_ + rollingSize - 1));
			segments.computeIfAbsent(hash, segment0 -> new ArrayList<>()).add(Segment.of(pos_, pos_ + rollingSize));
			hash = rh.unroll(hash, bytes.get(pos_), rollingSize);
		}

		return segments;
	}

}
