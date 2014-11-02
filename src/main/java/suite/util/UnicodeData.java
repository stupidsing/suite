package suite.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UnicodeData {

	private Map<String, Set<Character>> classByChars;

	public UnicodeData() {
		try (InputStream is = getClass().getResourceAsStream("UnicodeData.txt");
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			classByChars = br.lines() //
					.map(line -> line.split(";")) //
					.map(a -> Pair.of(a[2], (Character) (char) Integer.parseInt(a[0], 16))) //
					.collect(Collectors.groupingBy(p -> p.t0, Collectors.mapping(p -> p.t1, Collectors.toSet())));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Set<Character> getCharsOfClass(String uc) {
		return classByChars.get(uc);
	}

}
