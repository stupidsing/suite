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

import suite.debian.AptUtil.Repo;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class DependencyMain extends ExecutableProgram {

	private DebianUtil debianUtil = new DebianUtil();
	private DpkgUtil dpkgUtil = new DpkgUtil(debianUtil);
	private AptUtil aptUtil = new AptUtil(debianUtil);

	// Tools
	private List<String> mainList = Arrays.asList( //
			"abiword" //
			, "alsa-utils" //
			, "asunder" //
			, "bochs" //
			, "build-essential" //
			, "cifs-utils" //
			, "chromium" //
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
			, "gparted" //
			, "gpicview" //
			, "grub-pc" //
			, "imagemagick" //
			, "leafpad" //
			, "less" //
			, "libreadline-dev" //
			, "libreoffice" //
			, "lightdm" //
			, "mpg321" //
			, "netcat-traditional" //
			, "obconf" //
			, "openbox" //
			, "openjdk-8-jdk" //
			, "pcmanfm" //
			, "pidgin" //
			, "pidgin-hotkeys" //
			, "rdesktop" //
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
			, "unetbootin" //
			, "unzip" //
			, "usbutils" // lsusb
			, "vim" //
			, "virtualbox-dkms" //
			, "virtualbox-qt" //
			, "w3m" //
			, "wine" //
			, "wine32" //
			, "xchm" //
			, "xpdf" //
			, "xscavenger" //
			, "xserver-xorg" //
			, "yeahconsole" //
			, "zip" //
	);

	private List<String> debianList = Arrays.asList( //
			"icedove" // firefox
			, "iceweasel" // thunderbird
	);

	// Not a must, but good to have
	private List<String> supplementaryList = Arrays.asList( //
			"btrfs-tools" //
			, "eject" //
			, "gnupg2" //
			, "gstreamer1.0-plugins-good" //
			, "python-imaging" // for bitmap2ttf
	);

	private List<String> operatingSystemList = Arrays.asList( //
			"iamerican" //
			, "ibritish" //
	);

	private Set<String> requiredList = new HashSet<>(Util.add( //
			mainList //
			, debianList //
			, supplementaryList //
			, operatingSystemList //
	));

	public static void main(String args[]) {
		Util.run(DependencyMain.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		Read.from(getClass().getMethods()) //
				.filter(m -> m.getName().startsWith("list") && m.getParameters().length == 0) //
				.sink(m -> {
					System.out.println(m.getName() + "()");
					for (Object object : Rethrow.ex(() -> (List<?>) m.invoke(this, new Object[] {})))
						System.out.println(object);
					System.out.println();
					System.out.println();
				});
		return true;
	}

	public List<String> listDependeesOfDkms() {
		Repo repo = new Repo("http://mirrors.kernel.org/ubuntu" //
				, "xenial" //
				, "main" //
				, "amd64");
		String packageName = "dkms";

		List<Map<String, String>> packages;
		packages = Rethrow.ioException(() -> aptUtil.readRepoPackages(repo));
		Set<String> required = new HashSet<>(Arrays.asList(packageName));
		Set<String> required1 = dpkgUtil.getDependingSet(packages, required);
		return Read.from(required1) //
				.map(packageName_ -> aptUtil.getDownloadUrl(repo, packages, packageName_)) //
				.sort(Util.comparator()) //
				.toList();
	}

	public List<String> listManuallyInstalled() {
		return aptUtil.readManuallyInstalled().toList();
	}

	public List<String> listUndependedPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		Map<String, List<String>> dependees = dpkgUtil.getDependers(packages);

		return Read.from(packages) //
				.filter(pm -> !isEssential(pm)) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> !dependees.containsKey(packageName)) //
				.filter(packageName -> !requiredList.contains(packageName)) //
				.sort(Util.comparator()) //
				.toList();
	}

	public List<String> listUnusedPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		Set<String> required = new HashSet<>(requiredList);

		required.addAll(Read.from(packages) //
				.filter(pm -> isEssential(pm)) //
				.map(pm -> pm.get("Package")) //
				.toList());

		Set<String> required1 = dpkgUtil.getDependingSet(packages, required);

		return Read.from(packages) //
				.map(pm -> pm.get("Package")) //
				.filter(packageName -> !required1.contains(packageName)) //
				.sort(Util.comparator()) //
				.toList();
	}

	public List<String> listUnusedFiles() {
		Set<String> files = Read.from(dpkgUtil.readInstalledPackages()) //
				.concatMap(dpkgUtil::readFileList) //
				.toSet();

		return Read.from("/etc", "/usr") //
				.concatMap(p -> FileUtil.findPaths(Paths.get(p))) //
				.map(Path::toString) //
				.filter(p -> !files.contains(p)) //
				.toList();
	}

	private boolean isEssential(Map<String, String> pm) {
		return Objects.equals(pm.get("Essential"), "yes") //
				|| Arrays.asList("important", "required").contains(pm.get("Priority"));
	}

}
