package suite.debian;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;

public class DebianUtil {

	public Streamlet<Map<String, String>> readDpkgConfiguration(File file) {
		try (InputStream is = new FileInputStream(file)) {
			return readDpkgConfiguration(is);
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

	public Streamlet<Map<String, String>> readDpkgConfiguration(InputStream is) throws IOException {
		try (Reader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr)) {
			List<Map<String, String>> pms = new ArrayList<>();
			Map<String, String> pm = new HashMap<>();
			var sb = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith(" ") && 0 < sb.length()) {
					var kv = sb.toString();
					var pos = kv.indexOf(':');
					pm.put(kv.substring(0, pos).trim(), kv.substring(pos + 1).trim());
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
