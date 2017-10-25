package suite.lcs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class LcsSesMyersTest {

	@Test
	public void test0() {
		List<Integer> l0 = List.of(1);
		List<Integer> l1 = List.of(1);
		System.out.println(new LcsSesMyers<Integer>().myers(l0, l1));
	}

	@Test
	public void test() {
		List<Integer> l0 = List.of(1, 3, 5, 7, 9);
		List<Integer> l1 = List.of(2, 3, 5, 9, 11);
		List<Integer> answer = List.of(3, 5, 9);
		assertEquals(answer, new LcsSesMyers<Integer>().myers(l0, l1));
	}

}
