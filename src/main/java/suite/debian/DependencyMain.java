package suite.debian;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FileUtil;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class DependencyMain extends ExecutableProgram {

	private DebianUtil debianUtil = new DebianUtil();
	private DpkgUtil dpkgUtil = new DpkgUtil(debianUtil);
	private AptUtil aptUtil = new AptUtil(debianUtil);

	private Set<String> requiredList = new HashSet<>(Arrays.asList( //
			"abiword" //
			, "asunder" //
			, "bochs" //
			, "build-essential" //
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
			, "netcat-traditional" //
			, "obconf" //
			, "openbox" //
			, "openjdk-8-jdk" //
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
			, "ttf-dejavu" //
			, "ttf-wqy-zenhei" //
			, "unetbootin" //
			, "unzip" //
			, "usbutils" // lsusb
			, "vim" //
			, "virtualbox" //
			, "virtualbox-dkms" //
			, "w3m" //
			, "wine" //
			, "wine32" //
			, "xchm" //
			, "xpdf" //
			, "xscavenger" //
			, "xserver-xorg" //
			, "yeahconsole" //
			, "zip" //
	));

	public static void main(String args[]) {
		Util.run(DependencyMain.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		listManuallyInstalled();
		listUnusedPackages();
		listUnusedFiles();
		return true;
	}

	private void listManuallyInstalled() {
		Streamlet<String> readManuallyInstalled = aptUtil.readManuallyInstalled();
		readManuallyInstalled //
				.forEach(System.out::println);
	}

	private void listUnusedPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		Set<String> required = new HashSet<>(requiredList);

		required.addAll(Read.from(packages) //
				.filter(pm -> Objects.equals(pm.get("Essential"), "yes") || Objects.equals(pm.get("Priority"), "important")) //
				.map(pm -> pm.get("Package")) //
				.toList());

		Set<String> required1 = dpkgUtil.getDependingSet(packages, required);

		List<String> unusedPackageNames = Read.from(packages) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> !required1.contains(packageName)) //
				.sort(Util.comparator()) //
				.toList();

		unusedPackageNames.forEach(System.out::println);
	}

	private void listUnusedFiles() {
		Set<String> files = Read.from(dpkgUtil.readInstalledPackages()) //
				.concatMap(dpkgUtil::readFileList) //
				.toSet();

		List<String> unusedFiles = Read.from("/etc", "/usr") //
				.concatMap(p -> FileUtil.findPaths(Paths.get(p))) //
				.map(Path::toString) //
				.filter(p -> !files.contains(p)) //
				.toList();

		unusedFiles.forEach(System.out::println);
	}

}
