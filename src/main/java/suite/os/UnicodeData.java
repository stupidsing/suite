package suite.os;

import static primal.statics.Fail.fail;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.ReadLines;
import primal.adt.Pair;
import suite.streamlet.As;

public class UnicodeData {

	private Map<String, Set<Character>> classByChars;

	public UnicodeData() {
		try (var is = getClass().getResourceAsStream("UnicodeData.txt")) {
			classByChars = ReadLines //
					.from(is) //
					.map(line -> line.split(";")) //
					.map(a -> Pair.of(a[2], (char) Integer.parseInt(a[0], 16))) //
					.collect(As::setMap);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	public char[][] brailles(int[][] pixels) {
		var brailles = new char[pixels.length / 4][pixels[0].length / 2];

		for (var y = 0; y < pixels.length; y += 4)
			for (var x = 0; x < pixels[y].length; x += 2) {
				var i = 0;
				i = (i << 1) + pixels[y + 3][x + 1];
				i = (i << 1) + pixels[y + 3][x + 0];
				i = (i << 1) + pixels[y + 2][x + 1];
				i = (i << 1) + pixels[y + 1][x + 1];
				i = (i << 1) + pixels[y + 0][x + 1];
				i = (i << 1) + pixels[y + 2][x + 0];
				i = (i << 1) + pixels[y + 1][x + 0];
				i = (i << 1) + pixels[y + 0][x + 0];
				brailles[y / 4][x / 2] = (char) (0x2800 + i);
			}

		return brailles;
	}

	public Set<Character> getCharsOfClass(String uc) {
		return classByChars.get(uc);
	}

}
