package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import suite.primitive.Chars_;

public class IRopeTest {

	@Test
	public void test() {
		int length = 1024;
		String s = new String(Chars_.toArray(length, c -> (char) c));
		IRope<Character> rope = new IRope<>(IRope.ropeList(""));
		int p = 0;

		while (p < length) {
			int p1 = Math.min(length, p + 32 + new Random().nextInt(16));
			rope = IRope.meld(rope, new IRope<>(IRope.ropeList(s.substring(p, p1))));
			p = p1;
		}

		for (int i = 0; i < rope.weight; i++)
			assertEquals(i, rope.at(i).charValue());

		int halfLength = length / 2;
		IRope<Character> rope0 = rope.left(halfLength);
		IRope<Character> rope1 = rope.right(halfLength);

		for (int i = 0; i < halfLength; i++) {
			int l = i;
			int r = l + halfLength;
			assertEquals(l, rope0.at(l).charValue());
			assertEquals(r, rope1.at(l).charValue());
		}
	}

}
