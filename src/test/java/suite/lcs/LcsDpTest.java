package suite.lcs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LcsDpTest {

	@Test
	public void test() {
		var l0 = List.of(1, 3, 5, 7, 9);
		var l1 = List.of(2, 3, 5, 9, 11);
		var answer = List.of(3, 5, 9);
		assertEquals(answer, new LcsDp<Integer>().lcs(l0, l1));
	}

}
