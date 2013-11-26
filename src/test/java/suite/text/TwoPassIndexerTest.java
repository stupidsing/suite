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

import suite.text.TwoPassIndexer.Key;
import suite.util.FileUtil;
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

		Map<String, List<Key>> map = indexer.getKeysByWord();

		List<Entry<String, List<Key>>> entries = Util.sort(map.entrySet(), new Comparator<Entry<String, List<Key>>>() {
			public int compare(Entry<String, List<Key>> entry0, Entry<String, List<Key>> entry1) {
				return entry1.getValue().size() - entry0.getValue().size();
			}
		});

		for (int i = 0; i < 32; i++) {
			Entry<String, List<Key>> entry = entries.get(i);
			System.out.println(String.format("%-5d \"%s\"", entry.getValue().size(), entry.getKey()));
		}
	}

	private String readFile(String filename) throws IOException {
		StringBuilder sb = new StringBuilder();

		try (FileInputStream fis = new FileInputStream(filename);
				InputStreamReader isr = new InputStreamReader(fis, FileUtil.charset);
				BufferedReader br = new BufferedReader(isr)) {
			int size = 4096, nCharsRead;
			char buffer[] = new char[size];

			while ((nCharsRead = br.read(buffer)) >= 0)
				for (int index = 0; index < nCharsRead; index++) {
					char ch = buffer[index];

					// Fit in trie range; ignore control characters
					if (32 <= ch && ch < 128)
						sb.append(ch);
				}
		}

		return sb.toString();
	}

}
