package suite.persistent;

import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import suite.os.FileUtil;
import suite.persistent.PerRope.IRopeList;
import suite.primitive.Chars_;

public class PerRopeTest {

	@Test
	public void test() {
		var length = 1024;
		var s = new String(Chars_.toArray(length, c -> (char) c));
		var rope = new PerRope<>(IRopeList.of(""));
		var p = 0;

		while (p < length) {
			var p1 = min(length, p + 32 + new Random().nextInt(16));
			rope = PerRope.meld(rope, new PerRope<>(IRopeList.of(s.substring(p, p1))));
			p = p1;
		}

		for (var i = 0; i < length; i++)
			assertEquals(i, rope.at(i).charValue());

		var halfLength = length / 2;
		var rope0 = rope.left(halfLength);
		var rope1 = rope.right(halfLength);

		for (var i = 0; i < halfLength; i++) {
			var l = i;
			var r = l + halfLength;
			assertEquals(l, rope0.at(l).charValue());
			assertEquals(r, rope1.at(l).charValue());
		}
	}

	@Test
	public void testFile() {
		var inputText = IRopeList.of(FileUtil.read("src/main/java/suite/sample/DevMain.java"));
		inputText.right(inputText.size);
	}

}
