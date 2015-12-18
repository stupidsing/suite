package suite.debian;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class DpkgUtil {

	private String dpkgDir = "/var/lib/dpkg";

	private DebianUtil debianUtil = new DebianUtil();

	public DpkgUtil(DebianUtil debianUtil) {
		this.debianUtil = debianUtil;
	}

	public List<Map<String, String>> readInstalledPackages() {
		return debianUtil.readDpkgConfiguration(new File(dpkgDir + "/status")).toList();
	}

	public Streamlet<String> readFileList(Map<String, String> pm) {
		String packageName = pm.get("Package");
		String arch = pm.get("Architecture");
		String dir = dpkgDir + "/info/";

		List<File> files = new ArrayList<>();
		files.add(new File(dir + packageName + ".list"));
		if (arch != null)
			files.add(new File(dir + packageName + ":" + arch + ".list"));

		for (File file : files)
			if (file.exists())
				try {
					return Read.lines(file);
				} catch (FileNotFoundException ex) {
					throw new RuntimeException(ex);
				}

		return null;
	}

	public Set<String> getDependingSet(List<Map<String, String>> packages, Set<String> set0) {
		Map<String, List<String>> dependees = getDependees(packages);

		List<String> nl = new ArrayList<>(set0);
		Set<String> set1 = new HashSet<>(set0);

		while (!nl.isEmpty()) {
			String p = nl.remove(nl.size() - 1);
			List<String> list = dependees.get(p);

			if (list != null)
				for (String np : list)
					if (set1.add(np))
						nl.add(np);
		}

		return set1;
	}

	public Map<String, List<String>> getDependees(List<Map<String, String>> packages) {
		return Read.from(packages) //
				.map(pm -> {
					String line = pm.getOrDefault("Depends", "");
					List<String> list = Read.from(line.split(",")) //
							.filter(s -> !s.isEmpty()) //
							.map(s -> s.trim().split(" ")[0]) //
							.toList();
					return Pair.of(pm.get("Package"), list);
				}) //
				.collect(As::map);
	}

	public Map<String, List<String>> getDependers(List<Map<String, String>> packages) {
		return Read.multimap(getDependees(packages)) //
				.mapEntry((k, v) -> v, (k, v) -> k) //
				.toListMap();
	}

}
