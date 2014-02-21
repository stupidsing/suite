package suite.text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import suite.text.TwoPassIndexer.Reference;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class TwoPassIndexerTest {

	@Test
	public void test() throws IOException {
		Source<File> files0 = FileUtil.findFiles(new File("src/test/java"));

		Source<String> files1 = FunUtil.map(new Fun<File, String>() {
			public String apply(File file) {
				return file.getAbsolutePath();
			}
		}, files0);

		Source<String> files2 = FunUtil.filter(new Fun<String, Boolean>() {
			public Boolean apply(String filename) {
				return filename.endsWith(".java");
			}
		}, files1);

		List<String> filenames = new ArrayList<>();

		for (String filename : FunUtil.iter(files2))
			filenames.add(filename);

		TwoPassIndexer indexer = new TwoPassIndexer();

		for (String filename : filenames)
			indexer.pass0(filename, To.string(new File(filename)));

		for (String filename : filenames)
			indexer.pass1(filename, To.string(new File(filename)));

		Map<String, List<Reference>> map = indexer.getKeysByWord();

		List<Entry<String, List<Reference>>> entries = Util.sort(map.entrySet(), new Comparator<Entry<String, List<Reference>>>() {
			public int compare(Entry<String, List<Reference>> entry0, Entry<String, List<Reference>> entry1) {
				return entry1.getValue().size() - entry0.getValue().size();
			}
		});

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
