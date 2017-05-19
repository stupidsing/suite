package suite.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;

public class UnicodeData {

	private Map<String, Set<Character>> classByChars;

	public UnicodeData() {
		try (InputStream is = getClass().getResourceAsStream("UnicodeData.txt");
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			classByChars = Read.lines(is) //
					.map(line -> line.split(";")) //
					.map(a -> Pair.of(a[2], (char) Integer.parseInt(a[0], 16))) //
					.collect(As::setMap);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Set<Character> getCharsOfClass(String uc) {
		return classByChars.get(uc);
	}

}
