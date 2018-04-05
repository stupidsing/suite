package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.immutable.ISet;
import suite.os.FileUtil;
import suite.search.DirectedGraph;
import suite.search.StronglyConnectedComponents;
import suite.streamlet.Read;
import suite.util.Object_;
import suite.util.Rethrow;

public class DependencyTest {

	private String dir = FileUtil.homeDir();
	private List<String> sourceDirs = List.of(dir + "/src/main/java", dir + "/src/test/java");

	@Test
	public void testDependency() {
		dumpDependencies(ISet.empty(), "", "suite.util.Rethrow");
	}

	@Test
	public void testStronglyConnectedComponents() {
		var classes = new ArrayDeque<>(List.of("suite.jdk.DependencyTest"));
		Map<String, List<String>> dependenciesByClassName = new HashMap<>();
		String className;

		while ((className = classes.pollLast()) != null) {
			List<String> dependencies = getDependencies(className);
			dependenciesByClassName.put(className, dependencies);
			for (var className1 : dependencies)
				if (!dependenciesByClassName.containsKey(className1))
					classes.addLast(className1);
		}

		var vertices = dependenciesByClassName.keySet();

		Set<Pair<String, String>> edges = Read //
				.from2(dependenciesByClassName) //
				.concatMapValue(dependencies -> Read.from(dependencies)) //
				.toSet();

		StronglyConnectedComponents<String> scc = new StronglyConnectedComponents<>(DirectedGraph.of(vertices, edges));

		for (Set<Set<String>> layer : scc.group().layers()) {
			Read.from(layer).flatMap(iterable -> iterable).sort(Object_::compare).forEach(System.out::println);
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

		return Read //
				.from(sourceDirs) //
				.map(sourceDir -> Paths.get(sourceDir + "/" + p)) //
				.filter(path -> Files.exists(path)) //
				.concatMap(path -> Rethrow.ex(() -> Read.from(Files.readAllLines(path)))) //
				.filter(line -> line.startsWith("import ")) //
				.map(line -> line.split(" ")[1].replace(";", "")) //
				.toList();
	}

}
