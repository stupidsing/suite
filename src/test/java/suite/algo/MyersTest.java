package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MyersTest {

	@Test
	public void test0() {
		List<Integer> l0 = Arrays.asList(1);
		List<Integer> l1 = Arrays.asList(1);
		System.out.println(new Myers<Integer>().myers(l0, l1));
	}

	@Test
	public void test() {
		List<Integer> l0 = Arrays.asList(1, 3, 5, 7, 9);
		List<Integer> l1 = Arrays.asList(2, 3, 5, 9, 11);
		List<Integer> answer = Arrays.asList(3, 5, 9);
		assertEquals(answer, new Myers<Integer>().myers(l0, l1));
	}

}
