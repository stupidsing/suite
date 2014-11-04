package suite.debian;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Pair;

public class DependencyTest {

	@Test
	public void test() {
		File files[] = new File("/var/lib/apt/lists").listFiles();

		List<Map<String, String>> packages = Read.from(files) //
				.filter(File::isFile) //
				.concatMap(this::readPackagesFile) //
				.toList();

		// TODO get installed packages only

		Map<String, List<String>> dependBy = Read.from(packages) //
				.concatMap(pm -> {
					String depender = pm.get("Package");
					Streamlet<Pair<String, String>> t = Read.from(pm.get("Depends").split(",")) //
							.map(s -> s.split("(")[0].trim()) //
							.map(dependee -> Pair.of(dependee, depender));
					return t;
				}) //
				.collect(As.listMap());

		List<String> rootPackageNames = Read.from(packages) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> dependBy.get(packageName) == null) //
				.toList();

		System.out.println(rootPackageNames);
	}

	private Streamlet<Map<String, String>> readPackagesFile(File file) {
		try (InputStream is = new FileInputStream(file);
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			String line;
			List<Map<String, String>> pms = new ArrayList<>();
			Map<String, String> pm = new HashMap<>();

			while ((line = br.readLine()) != null)
				if (!line.isEmpty()) {
					int pos = line.indexOf(':');
					pm.put(line.substring(0, pos).trim(), line.substring(pos + 1).trim());
				} else {
					pms.add(pm);
					pm = new HashMap<>();
				}

			return Read.from(pms);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
