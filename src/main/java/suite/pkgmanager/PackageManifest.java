package suite.pkgmanager;

import java.util.Map;

public class PackageManifest {

	private String name;
	private String version;
	private Map<String, String> filenameMappings;

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public Map<String, String> getFilenameMappings() {
		return filenameMappings;
	}

}
