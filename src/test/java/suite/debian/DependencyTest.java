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
		List<Map<String, String>> packages = readPackagesFile(new File("/var/lib/dpkg/status")).toList();

		Map<String, List<String>> dependBy = Read.from(packages) //
				.concatMap(pm -> {
					String depender = pm.get("Package");
					String line = pm.getOrDefault("Depends", "");
					Streamlet<Pair<String, String>> t = Read.from(line.split(",")) //
							.map(s -> s.trim().split(" ")[0]) //
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

	@SuppressWarnings("unused")
	private List<Map<String, String>> readAllRepositoryIndices() {
		File files[] = new File("/var/lib/apt/lists").listFiles();

		return Read.from(files) //
				.filter(File::isFile) //
				.concatMap(this::readPackagesFile) //
				.toList();
	}

	private Streamlet<Map<String, String>> readPackagesFile(File file) {
		try (InputStream is = new FileInputStream(file);
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			List<Map<String, String>> pms = new ArrayList<>();
			Map<String, String> pm = new HashMap<>();
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith(" ") && sb.length() > 0) {
					String kv = sb.toString();
					int pos = kv.indexOf(':');
					pm.put(kv.substring(0, pos).trim(), kv.substring(pos + 1).trim());
					sb.setLength(0);
				}

				if (!line.isEmpty())
					sb.append(line.trim() + "\n");
				else {
					pms.add(pm);
					pm = new HashMap<>();
				}
			}

			return Read.from(pms);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
