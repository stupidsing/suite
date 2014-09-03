package suite.text;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;

import suite.text.TwoPassIndexer.Reference;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.To;
import suite.util.Util;

public class TwoPassIndexerTest {

	@Test
	public void test() throws IOException {
		List<String> filenames = To.list(FileUtil.findFiles(new File("src/test/java"))).stream() //
				.map(File::getAbsolutePath) //
				.filter(filename -> filename.endsWith(".java")) //
				.collect(Collectors.toList());

		TwoPassIndexer indexer = new TwoPassIndexer();

		for (String filename : filenames)
			indexer.pass0(filename, Util.read(filename));

		for (String filename : filenames)
			indexer.pass1(filename, Util.read(filename));

		Map<String, List<Reference>> map = indexer.getKeysByWord();

		List<Entry<String, List<Reference>>> entries = Util.sort(map.entrySet() //
				, (e0, e1) -> e1.getValue().size() - e0.getValue().size());

		System.out.println("Most popular key words:");

		for (int i = 0; i < 32; i++) {
			Entry<String, List<Reference>> entry = entries.get(i);
			System.out.println(String.format("%-5d \"%s\"", entry.getValue().size(), entry.getKey()));
		}

		System.out.println();

		for (Reference key : FunUtil.iter(indexer.search("IOException")))
			System.out.println("IOException found in " + key);
	}
}
