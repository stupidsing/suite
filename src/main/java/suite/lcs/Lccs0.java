package suite.lcs;

import static java.lang.Math.min;

import primal.Verbs.Equals;
import primal.adt.Pair;
import primal.primitive.IntMoreVerbs.IntersectInt;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.pair.IntObjPair;
import suite.text.RollingHash;
import suite.text.Segment;

/**
 * Longest common continuous subsequence, using a simple rolling hash method.
 *
 * @author ywsing
 */
public class Lccs0 {

	private RollingHash rh = new RollingHash();

	public Pair<Segment, Segment> lccs(Bytes bytes0, Bytes bytes1) {
		var size0 = bytes0.size();
		var size1 = bytes1.size();
		var rollingSize = min(size0, size1);

		if (0 < rollingSize) {
			var longest = IntObjPair.<Pair<Segment, Segment>> of(Integer.MIN_VALUE, null);

			while (longest.v == null) {
				var segments0 = hashSegments(bytes0, rollingSize);
				var segments1 = hashSegments(bytes1, rollingSize);
				var keys0 = ReadInt.from2(segments0).keys().toSet();
				var keys1 = ReadInt.from2(segments1).keys().toSet();
				var keys = ReadInt.from(IntersectInt.of(keys0, keys1)).toArray();

				for (var key : keys) {
					var segment0 = segments0.get(key);
					var segment1 = segments1.get(key);
					var end0 = segment0.end;
					var end1 = segment1.end;
					var b0 = bytes0.range(segment0.start, end0);
					var b1 = bytes1.range(segment1.start, end1);

					if (Equals.ab(b0, b1)) {
						var i = 0;
						int p0, p1;
						while ((p0 = end0 + i) < size0 && (p1 = end1 + i) < size1 && b0.get(p0) == b1.get(p1))
							i++;
						if (longest.k < i)
							longest.update(i, Pair.of(segment0, segment1));
					}
				}

				rollingSize--;
			}

			return longest.v;
		} else
			return Pair.of(Segment.of(0, 0), Segment.of(0, 0));
	}

	private IntObjMap<Segment> hashSegments(Bytes bytes, int rollingSize) {
		var segments = new IntObjMap<Segment>();
		var hash = rh.hash(bytes.range(0, rollingSize - 1));
		var size = bytes.size();

		for (var pos = 0; pos <= size - rollingSize; pos++) {
			hash = rh.roll(hash, bytes.get(pos + rollingSize - 1));
			segments.put(hash, Segment.of(pos, pos + rollingSize));
			hash = rh.unroll(hash, bytes.get(pos), rollingSize);
		}

		return segments;
	}

}
