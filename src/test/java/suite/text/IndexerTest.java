package suite.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.text.Indexer.Key;
import suite.util.FileUtil;
import suite.util.Util;

public class IndexerTest {

	@Test
	public void test() throws IOException {
		Indexer indexer = new Indexer();

		for (File file : FileUtil.findFiles(new File("src/test/java"))) {
			String filename = file.getAbsolutePath();

			if (filename.endsWith(".java")) {
				StringBuilder sb = new StringBuilder();

				try (FileInputStream fis = new FileInputStream(file);
						InputStreamReader isr = new InputStreamReader(fis, FileUtil.charset);
						BufferedReader br = new BufferedReader(isr)) {
					int size = 4096, nCharsRead;
					char buffer[] = new char[size];

					while ((nCharsRead = br.read(buffer)) >= 0)
						sb.append(buffer, 0, nCharsRead);
				}

				indexer.index(filename, sb.toString());
			}

			Map<String, List<Key>> map = indexer.getMap();

			for (String key : Util.sort(map.keySet())) {
				int n = map.get(key).size();
				if (n > 1 && key.length() < 8)
					System.out.println(String.format("%-5d \"%s\"", n, key));
			}
		}
	}

}
