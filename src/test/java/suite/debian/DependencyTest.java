package suite.debian;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

public class DependencyTest {

	@Test
	public void test() {
		Arrays.asList(new File("/var/lib/apt/lists").listFiles()).stream() //
				.filter(File::isFile) //
				.map(this::readPackagesFile) //
				.collect(Collectors.toList());
	}

	private Map<String, Map<String, String>> readPackagesFile(File file) {
		try (InputStream is = new FileInputStream(file);
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			String line;
			Map<String, Map<String, String>> maps = new HashMap<>();
			Map<String, String> map = new HashMap<>();

			while ((line = br.readLine()) != null)
				if (!line.isEmpty()) {
					int pos = line.indexOf(':');
					map.put(line.substring(0, pos).trim(), line.substring(pos + 1).trim());
				} else {
					maps.put(map.get("Package"), map);
					map = new HashMap<>();
				}

			return maps;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
