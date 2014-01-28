package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.net.Bytes;
import suite.text.Segment;
import suite.util.FileUtil;
import suite.util.Pair;

public class LongestCommonContinuousSequenceTest {

	@Test
	public void test() {
		LongestCommonContinuousSequence lccs = new LongestCommonContinuousSequence();
		List<Pair<Segment, Segment>> results = lccs.lccs(bytes("abczzzzz"), bytes("zzzzzabc"));
		assertEquals(1, results.size());

		Pair<Segment, Segment> result = results.get(0);
		assertEquals(3, result.t0.getStart());
		assertEquals(8, result.t0.getEnd());
		assertEquals(0, result.t1.getStart());
		assertEquals(5, result.t1.getEnd());
	}

	private Bytes bytes(String s) {
		return new Bytes(s.getBytes(FileUtil.charset));
	}

}
