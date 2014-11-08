package suite.debian;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class AptUtil {

	private String aptDir = "/var/lib/apt";

	private DebianUtil debianUtil;

	public AptUtil(DebianUtil debianUtil) {
		this.debianUtil = debianUtil;
	}

	public Streamlet<String> readManuallyInstalled() {
		return debianUtil.readDpkgConfiguration(new File(aptDir + "/extended_states")) //
				.filter(pm -> Objects.equals(pm.get("Auto-Installed"), "0")) //
				.map(pm -> pm.get("Package"));
	}

	public List<Map<String, String>> readRepositoryPackages() {
		File files[] = new File(aptDir + "/lists").listFiles();
		return Read.from(files) //
				.filter(File::isFile) //
				.filter(file -> file.getName().endsWith("_Packages")) //
				.concatMap(debianUtil::readDpkgConfiguration) //
				.toList();
	}

}
