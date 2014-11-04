package suite.debian;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Pair;
import suite.util.Util;

public class DependencyTest {

	private DebianUtil debianUtil = new DebianUtil();

	@Test
	public void testFindRootPackages() {
		List<Map<String, String>> packages = debianUtil.readDpkgConfiguration(new File("/var/lib/dpkg/status")).toList();

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
				.filter(pm -> !Objects.equals(pm.get("Essential"), "yes")) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> dependBy.get(packageName) == null) //
				.sort(Util.comparator()) //
				.toList();

		assertNotNull(rootPackageNames);
		rootPackageNames.forEach(System.out::println);
	}

	@Test
	public void testReadRepositoryIndices() {
		File files[] = new File("/var/lib/apt/lists").listFiles();

		assertNotNull(Read.from(files) //
				.filter(File::isFile) //
				.filter(file -> file.getName().endsWith("_Packages")) //
				.concatMap(debianUtil::readDpkgConfiguration) //
				.toList());
	}

}
