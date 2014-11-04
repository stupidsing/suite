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

public class DebianUtil {

	public Streamlet<Map<String, String>> readDpkgConfiguration(File file) {
		try (InputStream is = new FileInputStream(file);
				Reader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			List<Map<String, String>> pms = new ArrayList<>();
			Map<String, String> pm = new HashMap<>();
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith(" ") && sb.length() > 0) {
					String kv = sb.toString();
					int pos = kv.indexOf(':');
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
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
