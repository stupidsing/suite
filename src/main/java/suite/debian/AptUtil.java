package suite.debian;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.String_;
import suite.util.To;

public class AptUtil {

	private String aptDir = "/var/lib/apt";

	private DebianUtil debianUtil;

	public static class Repo {
		private String urlAddress;
		private String dist;
		private String tag;
		private String arch;

		public Repo(String urlAddress, String dist, String tag, String arch) {
			this.urlAddress = urlAddress;
			this.dist = dist;
			this.tag = tag;
			this.arch = arch;
		}
	}

	public AptUtil(DebianUtil debianUtil) {
		this.debianUtil = debianUtil;
	}

	public String getDownloadUrl(Repo repo, List<Map<String, String>> packages, String packageName) {
		String prefix = packageName.substring(0, packageName.startsWith("lib") ? 4 : 1);

		return Read.from(packages) //
				.filter(pm -> String_.equals(pm.get("Package"), packageName)) //
				.map(pm -> {
					String p = pm.get("Filename");
					if (p != null)
						return repo.urlAddress //
								+ "/" + p;
					else
						return repo.urlAddress //
								+ "/pool" //
								+ "/" + repo.tag //
								+ "/" + prefix //
								+ "/" + packageName //
								+ "/" + packageName + "_" + pm.get("Version") + "_" + pm.get("Architecture") + ".deb";
				}) //
				.uniqueResult();
	}

	public Streamlet<String> readManuallyInstalled() {
		return debianUtil.readDpkgConfiguration(new File(aptDir + "/extended_states")) //
				.filter(pm -> Objects.equals(pm.get("Auto-Installed"), "0")) //
				.map(pm -> pm.get("Package"));
	}

	public List<Map<String, String>> readRepoPackages() {
		File[] files = new File(aptDir + "/lists").listFiles();
		return Read.from(files) //
				.filter(File::isFile) //
				.filter(file -> file.getName().endsWith("_Packages")) //
				.concatMap(debianUtil::readDpkgConfiguration) //
				.toList();
	}

	public List<Map<String, String>> readRepoPackages(Repo repo) throws IOException {
		URL url = To.url(repo.urlAddress + "/dists/" + repo.dist + "/" + repo.tag + "/binary-" + repo.arch + "/Packages.gz");

		try (InputStream is = url.openConnection().getInputStream(); InputStream gis = new GZIPInputStream(is)) {
			return debianUtil.readDpkgConfiguration(gis).toList();
		}
	}

}
