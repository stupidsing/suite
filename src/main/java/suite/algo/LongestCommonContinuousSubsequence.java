package suite.algo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import suite.net.Bytes;
import suite.text.RollingHashUtil;
import suite.text.Segment;
import suite.util.Pair;
import suite.util.Util;

public class LongestCommonContinuousSubsequence {

	private RollingHashUtil rh = new RollingHashUtil();

	public Pair<Segment, Segment> lccs(Bytes bytes0, Bytes bytes1) {
		int rollingSize = Math.min(bytes0.size(), bytes1.size());
		Map<Integer, Segment> segments0 = createInitialHashes(bytes0, rollingSize);
		Map<Integer, Segment> segments1 = createInitialHashes(bytes1, rollingSize);

		while (true) {
			HashSet<Integer> keys = new HashSet<>(segments0.keySet());
			keys.retainAll(segments1.keySet());

			for (int key : keys) {
				Segment segment0 = segments0.get(key);
				Segment segment1 = segments1.get(key);
				Bytes b0 = bytes0.subbytes(segment0.getStart(), segment0.getEnd());
				Bytes b1 = bytes1.subbytes(segment1.getStart(), segment1.getEnd());
				if (Util.equals(b0, b1))
					return Pair.create(segment0, segment1);
			}

			segments0 = reduceSegments(segments0, bytes0, rollingSize);
			segments1 = reduceSegments(segments1, bytes1, rollingSize);
			rollingSize--;
		}
	}

	private Map<Integer, Segment> createInitialHashes(Bytes bytes, int rollingSize) {
		Map<Integer, Segment> segments = new HashMap<>();
		int hash = rh.hash(bytes.subbytes(0, rollingSize - 1));
		int size = bytes.size();

		for (int pos = 0; pos <= size - rollingSize; pos++) {
			hash = rh.roll(hash, bytes.byteAt(pos + rollingSize - 1));
			segments.put(hash, new Segment(pos, pos + rollingSize));
			hash = rh.unroll(hash, bytes.byteAt(pos), rollingSize);
		}

		return segments;
	}

	private Map<Integer, Segment> reduceSegments(Map<Integer, Segment> segments, Bytes bytes, int rollingSize) {
		Map<Integer, Segment> segments1 = new HashMap<>();

		for (Entry<Integer, Segment> entry : segments.entrySet()) {
			int hash = entry.getKey();
			Segment segment = entry.getValue();
			int start = segment.getStart(), end = segment.getEnd();

			segments1.put(rh.unroll(hash, bytes.byteAt(start), rollingSize), new Segment(start + 1, end));

			if (start == 0)
				segments1.put(rh.unroll(hash, bytes.byteAt(end - 1)), new Segment(start, end - 1));
		}

		return segments1;
	}

}
