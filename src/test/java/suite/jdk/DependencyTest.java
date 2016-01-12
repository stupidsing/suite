package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import suite.adt.Pair;
import suite.immutable.ISet;
import suite.os.FileUtil;
import suite.search.DirectedGraph;
import suite.search.StronglyConnectedComponents;
import suite.streamlet.Read;

public class DependencyTest {

	private String dir = FileUtil.homeDir();
	private List<String> sourceDirs = Arrays.asList(dir + "/src/main/java", dir + "/src/test/java");

	@Test
	public void testDependency() {
		dumpDependencies(new ISet<>(), "", "suite.jdk.DependencyTest");
	}

	@Test
	public void testStronglyConnectedComponents() {
		Deque<String> classes = new ArrayDeque<>(Arrays.asList("suite.jdk.DependencyTest"));
		Map<String, List<String>> dependenciesByClassName = new HashMap<>();
		String className;

		while ((className = classes.pollLast()) != null) {
			List<String> dependencies = getDependencies(className);
			dependenciesByClassName.put(className, dependencies);
			for (String className1 : dependencies)
				if (!dependenciesByClassName.containsKey(className1))
					classes.addLast(className1);
		}

		Set<String> vertices = dependenciesByClassName.keySet();

		Set<Pair<String, String>> edges = Read.from2(dependenciesByClassName) //
				.concatMapValue(dependencies -> Read.from(dependencies)) //
				.toSet();

		StronglyConnectedComponents<String> scc = new StronglyConnectedComponents<>(DirectedGraph.of(vertices, edges));

		List<Set<Set<String>>> layers = scc.group().layers();

		for (Set<Set<String>> layer : layers) {
			Read.from(layer).concatMap(Read::from).forEach(System.out::println);
			System.out.println();
		}
	}

	private void dumpDependencies(ISet<String> set, String indent, String className) {
		if (!set.contains(className)) {
			System.out.println(indent + className);

			ISet<String> set1 = set.add(className);
			getDependencies(className).forEach(className1 -> dumpDependencies(set1, indent + "  ", className1));
		}
	}

	private List<String> getDependencies(String className) {
		String p = className.replace('.', '/') + ".java";

		return Read.from(sourceDirs) //
				.map(sourceDir -> Paths.get(sourceDir + "/" + p)) //
				.filter(path -> Files.exists(path)) //
				.concatMap(path -> {
					try {
						return Read.from(Files.readAllLines(path));
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}) //
				.filter(line -> line.startsWith("import ")) //
				.map(line -> line.split(" ")[1].replace(";", "")) //
				.toList();
	}

}
