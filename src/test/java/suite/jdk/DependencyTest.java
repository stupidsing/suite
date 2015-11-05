package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.immutable.ISet;
import suite.os.FileUtil;
import suite.streamlet.Read;

public class DependencyTest {

	private String dir = FileUtil.homeDir();
	private List<String> sourceDirs = Arrays.asList(dir + "/src/main/java", dir + "/src/test/java");

	@Test
	public void testDependency() {
		dumpDependencies(new ISet<>(), "", "suite.jdk.DependencyTest");
	}

	private void dumpDependencies(ISet<String> set, String indent, String className) {
		if (!set.contains(className)) {
			ISet<String> set1 = set.add(className);

			String p = className.replace('.', '/') + ".java";

			Read.from(sourceDirs) //
					.map(sourceDir -> Paths.get(sourceDir + "/" + p)) //
					.filter(path -> Files.exists(path)) //
					.map(path -> {
						System.out.println(indent + className);
						return path;
					}) //
					.concatMap(path -> {
						try {
							return Read.from(Files.readAllLines(path));
						} catch (Exception ex) {
							throw new RuntimeException(ex);
						}
					}) //
					.filter(line -> line.startsWith("import ")) //
					.map(line -> line.split(" ")[1].replace(";", "")) //
					.forEach(className1 -> dumpDependencies(set1, indent + "  ", className1));
		}
	}

}
