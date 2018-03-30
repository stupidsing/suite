package suite.lcs;

import static suite.util.Friends.min;

import java.util.Objects;

import suite.adt.pair.Pair;
import suite.primitive.Bytes;
import suite.primitive.adt.map.IntObjMap;
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
		int rollingSize = min(bytes0.size(), bytes1.size());

		if (0 < rollingSize) {
			IntObjMap<Segment> segments0 = hashSegments(bytes0, rollingSize);
			IntObjMap<Segment> segments1 = hashSegments(bytes1, rollingSize);

			while (true) {
				IntSet keys0 = segments0.streamlet().keys().toSet();
				IntSet keys1 = segments1.streamlet().keys().toSet();
				int[] keys = IntSet.intersect(keys0, keys1).streamlet().toArray();

				for (int key : keys) {
					Segment segment0 = segments0.get(key);
					Segment segment1 = segments1.get(key);
					Bytes b0 = bytes0.range(segment0.start, segment0.end);
					Bytes b1 = bytes1.range(segment1.start, segment1.end);
					if (Objects.equals(b0, b1))
						return Pair.of(segment0, segment1);
				}

				segments0 = reduceSegments(segments0, bytes0, rollingSize);
				segments1 = reduceSegments(segments1, bytes1, rollingSize);
				rollingSize--;
			}
		} else
			return Pair.of(Segment.of(0, 0), Segment.of(0, 0));
	}

	private IntObjMap<Segment> hashSegments(Bytes bytes, int rollingSize) {
		IntObjMap<Segment> segments = new IntObjMap<>();
		int hash = rh.hash(bytes.range(0, rollingSize - 1));
		int size = bytes.size();

		for (int pos = 0; pos <= size - rollingSize; pos++) {
			hash = rh.roll(hash, bytes.get(pos + rollingSize - 1));
			segments.put(hash, Segment.of(pos, pos + rollingSize));
			hash = rh.unroll(hash, bytes.get(pos), rollingSize);
		}

		return segments;
	}

	private IntObjMap<Segment> reduceSegments(IntObjMap<Segment> segments0, Bytes bytes, int rollingSize) {
		IntObjMap<Segment> segments1 = new IntObjMap<>();

		segments0.forEach((hash, segment) -> {
			int start = segment.start, end = segment.end;

			segments1.update(rh.unroll(hash, bytes.get(start), rollingSize), segment0 -> Segment.of(start + 1, end));

			if (start == 0)
				segments1.update(rh.unroll(hash, bytes.get(end - 1)), segment0 -> Segment.of(start, end - 1));
		});

		return segments1;
	}

}
