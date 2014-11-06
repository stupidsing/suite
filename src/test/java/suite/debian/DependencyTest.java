package suite.debian;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Pair;
import suite.util.Util;

public class DependencyTest {

	private DebianUtil debianUtil = new DebianUtil();

	private Set<String> requiredList = new HashSet<>(Arrays.asList( //
			"abiword" //
			, "asunder" //
			, "cifs-utils" //
			, "compizconfig-settings-manager" //
			, "compiz-plugins" //
			, "deborphan" //
			, "dia" //
			, "dosbox" //
			, "flac" //
			, "fonts-droid" //
			, "fonts-inconsolata" //
			, "fonts-umeplus" //
			, "fontforge" //
			, "g++" //
			, "gcin" //
			, "gconf-editor" //
			, "git-core" //
			, "gnome-tweak-tool" //
			, "gnugo" //
			, "gnumeric" //
			, "golang" //
			, "google-chrome-stable" //
			, "gparted" //
			, "gpicview" //
			, "grub-pc" //
			, "icedove" //
			, "icedtea-7-plugin" //
			, "iceweasel" //
			, "leafpad" //
			, "less" //
			, "libreadline-dev" //
			, "libreoffice" //
			, "mpg321" //
			, "openbox" //
			, "openjdk-7-jdk" //
			, "pcmanfm" //
			, "pidgin" //
			, "pidgin-hotkeys" //
			, "rlwrap" //
			, "rsync" //
			, "rxvt-unicode" //
			, "scite" //
			, "ssh" //
			, "sshfs" //
			, "subversion" //
			, "supertux" //
			, "terminator" //
			, "thunderbird" //
			, "tilda" //
			, "tint2" //
			, "torcs" //
			, "ttf-wqy-zenhei" //
			, "unetbootin" //
			, "unzip" //
			, "vim" //
			, "virtualbox" //
			, "w3m" //
			, "wine" //
			, "xchm" //
			, "xpdf" //
			, "xscavenger" //
			, "yeahconsole" //
			, "zip" //
	));

	@Test
	public void testListManualInstalled() {
		debianUtil.readDpkgConfiguration(new File("/var/lib/apt/extended_states")) //
				.filter(pm -> Objects.equals(pm.get("Auto-Installed"), "0")) //
				.map(pm -> pm.get("Package")) //
				.forEach(System.out::println);
	}

	@Test
	public void testListRootPackages() {
		List<Map<String, String>> packages = debianUtil.readDpkgConfiguration(new File("/var/lib/dpkg/status")).toList();

		Map<String, List<String>> dependBy = Read.from(packages) //
				.concatMap(pm -> {
					String depender = pm.get("Package");
					String line = pm.getOrDefault("Depends", "");
					Streamlet<Pair<String, String>> t = Read.from(line.split(",")) //
							.map(s -> s.trim().split(" ")[0]) //
							.map(dependee -> Pair.of(dependee, depender));
					return t;
				}) //
				.collect(As.listMap());

		List<String> unnecessaryPackageNames = Read.from(packages) //
				.filter(pm -> !Objects.equals(pm.get("Essential"), "yes")) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> !requiredList.contains(packageName)) //
				.filter(packageName -> dependBy.get(packageName) == null) //
				.sort(Util.comparator()) //
				.toList();

		assertNotNull(unnecessaryPackageNames);
		unnecessaryPackageNames.forEach(System.out::println);
	}

	@Test
	public void testReadRepositoryIndices() {
		File files[] = new File("/var/lib/apt/lists").listFiles();

		assertNotNull(Read.from(files) //
				.filter(File::isFile) //
				.filter(file -> file.getName().endsWith("_Packages")) //
				.concatMap(debianUtil::readDpkgConfiguration) //
				.toList());
	}

}
