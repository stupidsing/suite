package suite.debian;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
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
		var packageName = pm.get("Package");
		var arch = pm.get("Architecture");
		var dir = dpkgDir + "/info/";

		var files = Read.each(new File(dir + packageName + ".list"));

		if (arch != null)
			files = files.snoc(new File(dir + packageName + ":" + arch + ".list"));

		var file = files.filter(File::exists).first();
		return file != null ? Read.lines(file) : null;
	}

	public Set<String> getDependeeSet(List<Map<String, String>> packages, Set<String> set0) {
		var dependees = getDependeesOf(packages);
		var nl = new ArrayList<>(set0);
		var set1 = new HashSet<>(set0);

		while (!nl.isEmpty()) {
			var p = nl.remove(nl.size() - 1);

			for (var np : dependees.get(p))
				if (set1.add(np))
					nl.add(np);
		}

		return set1;
	}

	public ListMultimap<String, String> getDependeesOf(List<Map<String, String>> packages) {
		return Read //
				.from(packages) //
				.map(pm -> {
					var line = pm.getOrDefault("Depends", "");
					var list = Read //
							.from(line.split(",")) //
							.filter(s -> !s.isEmpty()) //
							.map(s -> s.trim().split(" ")[0]) //
							.toList();
					return Pair.of(pm.get("Package"), list);
				}) //
				.map2(Pair::fst, Pair::snd) //
				.collect(As::multimap);
	}

	public Map<String, List<String>> getDependersOf(List<Map<String, String>> packages) {
		return getDependeesOf(packages).entries().map2((k, v) -> v, (k, v) -> k).toListMap();
	}

}
