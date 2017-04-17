package suite.debian;

import java.io.File;
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
				return Read.lines(file);

		return null;
	}

	public Set<String> getDependeeSet(List<Map<String, String>> packages, Set<String> set0) {
		Map<String, List<String>> dependees = getDependeesOf(packages);

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

	public Map<String, List<String>> getDependeesOf(List<Map<String, String>> packages) {
		return Read.from(packages) //
				.map(pm -> {
					String line = pm.getOrDefault("Depends", "");
					List<String> list = Read.from(line.split(",")) //
							.filter(s -> !s.isEmpty()) //
							.map(s -> s.trim().split(" ")[0]) //
							.toList();
					return Pair.of(pm.get("Package"), list);
				}) //
				.map2(pair -> pair.t0, pair -> pair.t1) //
				.collect(As::map);
	}

	public Map<String, List<String>> getDependersOf(List<Map<String, String>> packages) {
		return Read.multimap(getDependeesOf(packages)) //
				.map2((k, v) -> v, (k, v) -> k) //
				.toListMap();
	}

}
