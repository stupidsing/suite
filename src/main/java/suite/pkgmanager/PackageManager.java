package suite.pkgmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import suite.adt.Pair;
import suite.os.FileUtil;
import suite.pkgmanager.action.ExecCommandAction;
import suite.pkgmanager.action.ExtractFileAction;
import suite.pkgmanager.action.InstallAction;
import suite.streamlet.Read;
import suite.wildcard.WildcardUtil;

public class PackageManager {

	private ObjectMapper objectMapper = new ObjectMapper();
	private Keeper keeper = new Keeper(objectMapper);
	private Log log = LogFactory.getLog(getClass());

	public boolean install(String packageFilename) throws IOException {
		PackageManifest packageManifest = getPackageManifest(packageFilename);

		List<Pair<String, String>> filenameMappings = Read.from2(packageManifest.getFilenameMappings()) //
				.sort((p0, p1) -> p1.t0.length() - p0.t0.length()) //
				.toList();

		List<InstallAction> installActions = new ArrayList<>();

		try (ZipFile zipFile = new ZipFile(packageFilename)) {
			installActions.addAll(Read.from(FileUtil.listZip(zipFile)) //
					.map(filename0 -> {
						String filename1 = filename0;
						for (Pair<String, String> filenameMapping : filenameMappings) {
							String match[];
							if ((match = WildcardUtil.match(filenameMapping.t0, filename1)) != null) {
								filename1 = WildcardUtil.apply(filenameMapping.t1, match);
								break;
							}
						}
						return new ExtractFileAction(packageFilename, filename0, filename1);
					}) //
					.toList());
		}

		installActions.addAll(Read.from(packageManifest.getCommands()) //
				.map(command -> new ExecCommandAction(command.getInstallCommand(), command.getUninstallCommand())) //
				.toList());

		int progress = 0;
		boolean isSuccess = true;

		for (; progress < installActions.size(); progress++)
			try {
				installActions.get(progress).act();
			} catch (Exception ex) {
				log.error("Error during installation", ex);
				isSuccess = false;
				break;
			}

		if (isSuccess)
			keeper.savePackageMemento(new PackageMemento(packageManifest, installActions));
		else
			progress = unact(installActions, progress);

		return isSuccess;
	}

	public boolean uninstall(String packageName) throws IOException {
		PackageMemento packageMemento = keeper.loadPackageMemento(packageName);
		List<InstallAction> installActions = packageMemento.getInstallActions();
		unact(installActions, installActions.size());
		keeper.removePackageMemento(packageName);
		return true;
	}

	private int unact(List<InstallAction> installActions, int progress) {
		for (; 0 < progress; progress--)
			try {
				installActions.get(progress - 1).unact();
			} catch (Exception ex) {
				log.error("Error during un-installation", ex);
			}
		return progress;
	}

	private PackageManifest getPackageManifest(String packageFilename) throws IOException {
		try (ZipFile zipFile = new ZipFile(packageFilename);
				InputStream fis = zipFile.getInputStream(zipFile.getEntry("pm.json"))) {
			return objectMapper.readValue(fis, PackageManifest.class);
		}
	}

}
