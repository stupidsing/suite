package suite.text;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import primal.Verbs.ReadString;
import primal.Verbs.Sort;
import suite.os.FileUtil;

public class TwoPassIndexTest {

	@Test // long test
	public void test() {
		var filenames = FileUtil //
				.findPaths(Paths.get("src/test/java")) //
				.map(Path::toAbsolutePath) //
				.map(Path::toString) //
				.filter(filename -> filename.endsWith(".java")) //
				.toList();

		var indexer = new TwoPassIndex();

		for (var filename : filenames)
			indexer.pass0(filename, ReadString.from(filename));

		for (var filename : filenames)
			indexer.pass1(filename, ReadString.from(filename));

		var entries = Sort.list( //
				indexer.getReferencesByWord().entrySet(), //
				(e0, e1) -> e1.getValue().size() - e0.getValue().size());

		System.out.println("Most popular key words:");

		for (var i = 0; i < 32; i++) {
			var entry = entries.get(i);
			System.out.println(String.format("%-5d \"%s\"", entry.getValue().size(), entry.getKey()));
		}

		System.out.println();

		for (var key : indexer.search("IOException"))
			System.out.println("IOException found in " + key);
	}

}
