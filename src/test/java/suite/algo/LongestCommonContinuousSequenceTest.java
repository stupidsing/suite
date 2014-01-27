package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.fastlist.IntList;
import suite.util.FileUtil;

public class LongestCommonContinuousSequenceTest {

	@Test
	public void test() {
		LongestCommonContinuousSequence lccs = new LongestCommonContinuousSequence();
		List<IntList> results = lccs.lccs("abczzzzz".getBytes(FileUtil.charset), "zzzzzabc".getBytes(FileUtil.charset));
		assertEquals(1, results.size());

		IntList result = results.get(0);
		assertEquals(3, result.get(0));
		assertEquals(8, result.get(1));
		assertEquals(0, result.get(2));
		assertEquals(5, result.get(3));
	}

}
