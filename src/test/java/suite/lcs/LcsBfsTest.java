package suite.lcs;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LcsBfsTest {

	@Test
	public void test() {
		List<Integer> l0 = Arrays.asList(1, 3, 5, 7, 9);
		List<Integer> l1 = Arrays.asList(2, 3, 5, 9, 11);
		List<Integer> answer = Arrays.asList(3, 5, 9);
		assertEquals(answer, new LcsBfs<Integer>().lcs(l0, l1));
	}

}
