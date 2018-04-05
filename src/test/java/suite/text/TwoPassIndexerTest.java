package suite.text;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import suite.os.FileUtil;
import suite.text.TwoPassIndexer.Reference;
import suite.util.List_;

public class TwoPassIndexerTest {

	@Test
	public void test() {
		var filenames = FileUtil.findPaths(Paths.get("src/test/java")) //
				.map(Path::toAbsolutePath) //
				.map(Path::toString) //
				.filter(filename -> filename.endsWith(".java")) //
				.toList();

		var indexer = new TwoPassIndexer();

		for (var filename : filenames)
			indexer.pass0(filename, FileUtil.read(filename));

		for (var filename : filenames)
			indexer.pass1(filename, FileUtil.read(filename));

		var map = indexer.getReferencesByWord();

		var entries = List_.sort(map.entrySet() //
				, (e0, e1) -> e1.getValue().size() - e0.getValue().size());

		System.out.println("Most popular key words:");

		for (var i = 0; i < 32; i++) {
			Entry<String, List<Reference>> entry = entries.get(i);
			System.out.println(String.format("%-5d \"%s\"", entry.getValue().size(), entry.getKey()));
		}

		System.out.println();

		for (var key : indexer.search("IOException"))
			System.out.println("IOException found in " + key);
	}

}
