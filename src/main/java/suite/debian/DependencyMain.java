package suite.debian;

import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.debian.AptUtil.Repo;
import suite.object.Object_;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.Set_;

public class DependencyMain extends ExecutableProgram {

	private DebianUtil debianUtil = new DebianUtil();
	private DpkgUtil dpkgUtil = new DpkgUtil(debianUtil);
	private AptUtil aptUtil = new AptUtil(debianUtil);

	// tools
	private List<String> baseList = List.of( //
			"acpi", //
			"cifs-utils", //
			"deborphan", //
			"grub-pc", //
			"imagemagick", //
			"less", //
			"linux-headers-686-pae", //
			"linux-image-686-pae", //
			"manpages", //
			"netcat-traditional", //
			"rlwrap", //
			"rsync", //
			"ssh", //
			"sshfs", //
			"subversion", //
			"unzip", //
			"usbutils", // lsusb
			"vim", //
			"w3m", //
			"wpasupplicant", //
			"zip");

	private List<String> debianList = List.of( //
			"icedove", // firefox
			"iceweasel"); // thunderbird

	private List<String> devList = List.of( //
			"bochs", //
			"build-essential", //
			"g++", //
			"git-core", //
			"golang", //
			"libreadline-dev", //
			"openjdk-8-jdk");

	private List<String> gamesList = List.of( //
			"frogatto", //
			"gnugo", //
			"supertux", //
			"torcs", //
			"xscavenger");

	private List<String> guiList = List.of( //
			"abiword", //
			"asunder", //
			"evince", //
			"chromium", //
			"compizconfig-settings-manager", //
			"compiz-plugins", //
			"dia", //
			"dosbox", //
			"fonts-droid", //
			"fonts-inconsolata", //
			"fonts-umeplus", //
			"fontforge", //
			"gcin", //
			"gconf-editor", //
			"gnome-tweak-tool", //
			"gnumeric", //
			"gparted", //
			"gpicview", //
			"leafpad", //
			"libreoffice", //
			"lightdm", //
			"obconf", //
			"openbox", //
			"pcmanfm", //
			"pidgin", //
			"pidgin-hotkeys", //
			"rdesktop", //
			"rxvt-unicode", //
			"scite", //
			"terminator", //
			"thunderbird", //
			"tilda", //
			"tint2", //
			"unetbootin", //
			"virtualbox-dkms", //
			"virtualbox-qt", //
			"wine", //
			"wine32", //
			"xchm", //
			"xpdf", //
			"xserver-xorg", //
			"yeahconsole");

	private List<String> mediaList = List.of( //
			"alsa-utils", //
			"flac", //
			"mpg321");

	// not a must, but good to have
	private List<String> supplementaryList = List.of( //
			"btrfs-tools", //
			"eject", //
			"gnupg2", //
			"gstreamer1.0-plugins-good", //
			"python-imaging"); // for bitmap2ttf

	private List<String> operatingSystemList = List.of( //
			"iamerican", //
			"ibritish");

	private Set<String> requiredList = Set_.union( //
			baseList, //
			debianList, //
			devList, //
			gamesList, //
			guiList, //
			mediaList, //
			operatingSystemList, //
			supplementaryList);

	public static void main(String[] args) {
		RunUtil.run(DependencyMain.class, args);
	}

	protected boolean run(String[] args) throws IOException {
		Read //
				.from(getClass().getMethods()) //
				.filter(m -> m.getName().startsWith("list") && m.getParameters().length == 0) //
				.sink(m -> {
					System.out.println(m.getName() + "()");
					for (var object : rethrow(() -> (List<?>) m.invoke(this, new Object[] {})))
						System.out.println(object);
					System.out.println();
					System.out.println();
				});
		return true;
	}

	public List<String> listDeinstalledPackages() {
		var packages = dpkgUtil.readInstalledPackages();
		return Read //
				.from(packages) //
				.filter(pm -> pm.get("Status").contains("deinstall")) //
				.map(pm -> "sudo dpkg --purge " + packageName(pm)) //
				.sort(Object_::compare) //
				.toList();
	}

	public List<String> listDependeesOfDkms() {
		var repo = new Repo("http://mirrors.kernel.org/ubuntu" //
				, "xenial" //
				, "main" //
				, "amd64");
		var packageName = "dkms";

		List<Map<String, String>> packages;
		packages = rethrow(() -> aptUtil.readRepoPackages(repo));
		var required = new HashSet<>(List.of(packageName));
		Set<String> required1 = dpkgUtil.getDependeeSet(packages, required);
		return Read //
				.from(required1) //
				.map(packageName_ -> aptUtil.getDownloadUrl(repo, packages, packageName_)) //
				.sort(Object_::compare) //
				.toList();
	}

	public List<String> listManuallyInstalled() {
		return aptUtil.readManuallyInstalled().toList();
	}

	public List<String> listUndependedPackages() {
		var packages = dpkgUtil.readInstalledPackages();
		var dependees = dpkgUtil.getDependersOf(packages);

		return Read //
				.from(packages) //
				.filter(pm -> !isEssential(pm)) //
				.map(this::packageName) //
				.filter(packageName -> !dependees.containsKey(packageName)) //
				.filter(packageName -> !requiredList.contains(packageName)) //
				.map(packageName -> "sudo apt remove -y --purge " + packageName) //
				.sort(Object_::compare) //
				.toList();
	}

	public List<String> listUnusedPackages() {
		var packages = dpkgUtil.readInstalledPackages();
		var required = new HashSet<>(requiredList);

		required.addAll(Read //
				.from(packages) //
				.filter(this::isEssential) //
				.map(this::packageName) //
				.toList());

		Set<String> required1 = dpkgUtil.getDependeeSet(packages, required);

		return Read //
				.from(packages) //
				.map(this::packageName) //
				.filter(packageName -> !required1.contains(packageName)) //
				.sort(Object_::compare) //
				.toList();
	}

	public List<String> listUnusedFiles() {
		var files = Read //
				.from(dpkgUtil.readInstalledPackages()) //
				.concatMap(dpkgUtil::readFileList) //
				.toSet();

		return Read //
				.each("/etc", "/usr") //
				.concatMap(p -> FileUtil.findPaths(Paths.get(p))) //
				.map(Path::toString) //
				.filter(p -> !files.contains(p)) //
				.toList();
	}

	private String packageName(Map<String, String> pm) {
		return pm.get("Package");
	}

	private boolean isEssential(Map<String, String> pm) {
		return Objects.equals(pm.get("Essential"), "yes") //
				|| List.of("important", "required").contains(pm.get("Priority"));
	}

}
