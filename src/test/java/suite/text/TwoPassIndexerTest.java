package suite.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import suite.text.TwoPassIndexer.Reference;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.To;
import suite.util.Util;

public class TwoPassIndexerTest {

	@Test
	public void test() throws IOException {
		TwoPassIndexer indexer = new TwoPassIndexer();

		List<String> filenames = new ArrayList<>();

		for (File file : FileUtil.findFiles(new File("src/test/java"))) {
			String filename = file.getAbsolutePath();

			if (filename.endsWith(".java"))
				filenames.add(filename);
		}

		for (String filename : filenames)
			indexer.pass0(filename, readFile(filename));

		for (String filename : filenames)
			indexer.pass1(filename, readFile(filename));

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

	private String readFile(String filename) throws IOException {
		try (FileInputStream fis = new FileInputStream(filename);
				InputStreamReader isr = new InputStreamReader(fis, FileUtil.charset);
				BufferedReader br = new BufferedReader(isr)) {
			return To.string(br);
		}
	}

}
