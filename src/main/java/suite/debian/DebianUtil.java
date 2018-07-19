package suite.debian;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.String_;

public class DebianUtil {

	public Streamlet<Map<String, String>> readDpkgConfiguration(File file) {
		return FileUtil.in(file.toString()).doRead(this::readDpkgConfiguration);
	}

	public Streamlet<Map<String, String>> readDpkgConfiguration(InputStream is) throws IOException {
		try (var isr = new InputStreamReader(is); var br = new BufferedReader(isr)) {
			var pms = new ArrayList<Map<String, String>>();
			var pm = new HashMap<String, String>();
			var sb = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith(" ") && 0 < sb.length()) {
					var pair = String_.split2(sb.toString(), ":");
					pm.put(pair.t0, pair.t1);
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
