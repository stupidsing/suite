package suite.debian;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import primal.MoreVerbs.Split;
import primal.Verbs.ReadFile;
import primal.streamlet.Streamlet;
import suite.streamlet.Read;

public class DebianUtil {

	public Streamlet<Map<String, String>> readDpkgConfiguration(File file) {
		return ReadFile.from(file.toString()).doRead(this::readDpkgConfiguration);
	}

	public Streamlet<Map<String, String>> readDpkgConfiguration(InputStream is) throws IOException {
		try (var isr = new InputStreamReader(is); var br = new BufferedReader(isr)) {
			var pms = new ArrayList<Map<String, String>>();
			var pm = new HashMap<String, String>();
			var sb = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith(" ") && 0 < sb.length()) {
					var pair = Split.string(sb.toString(), ":");
					pm.put(pair.k, pair.v);
					sb.setLength(0);
				}

				if (!line.isEmpty())
					sb.append(line.trim() + "\n");
				else {
					pms.add(pm);
					pm = new HashMap<>();
				}
			}

			return Read.from(pms);
		}
	}

}
