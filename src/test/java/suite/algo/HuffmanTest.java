package suite.algo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class HuffmanTest {

	private Huffman<Character> h = new Huffman<>();

	@Test
	public void test0() {
		test(Collections.emptyList());
	}

	@Test
	public void test1() {
		test(List.of('A'));
	}

	@Test
	public void test3() {
		test(List.of('A', 'B', 'C'));
	}

	@Test
	public void testn() {
		var expect = new ArrayList<Character>();
		for (var i = 0; i < 4096; i++)
			expect.add((char) ('A' + new Random().nextInt(26)));
		test(expect);
	}

	private void test(List<Character> expect) {
		var pair = h.encode(expect);
		var actual = h.decode(pair);
		assertEquals(expect, actual);
	}

}
