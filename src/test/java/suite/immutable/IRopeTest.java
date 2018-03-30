package suite.immutable;

import java.util.Random;

import org.junit.Test;

import suite.primitive.Chars_;

public class IRopeTest {

	@Test
	public void test() {
		String s = new String(Chars_.toArray(1024, c -> (char) c));
		IRope<Character> rope = new IRope<>(IRope.ropeList(""));

		for (int i = 0; i < s.length(); i++) {
			int i1 = Math.min(s.length(), i + 32 + new Random().nextInt(16));
			rope = IRope.meld(rope, new IRope<>(IRope.ropeList(s.substring(i, i1))));
			i = i1;
		}

		for (int i = 0; i < rope.weight; i++)
			System.out.println(rope.at(i));
		;
	}

}
